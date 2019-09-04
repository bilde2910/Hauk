#############################################################################################
# Internal Service Setup
#############################################################################################

require 'json'

CONFIG = JSON.parse(File.read('config.json'), symbolize_names: true)

SESSIONS = CONFIG[:use_session_store] && File.exist?(CONFIG[:session_store]) ?
            JSON.parse(File.read(CONFIG[:session_store]), symbolize_names: true).tap{|s| puts "loaded #{s.size} session(s)"} :
            {}
if CONFIG[:use_session_store]
  at_exit do
    if CONFIG[:session_store]
      SESSIONS.delete_if {|k, v| v[:expire] < Time.now.to_i }
      puts "saving #{SESSIONS.size} session(s)"
      File.write(CONFIG[:session_store], SESSIONS.to_json)
    end
  end
end

require 'sinatra'
require 'webrick/ssl'
require 'webrick/https'
require 'digest/md5'

SSL_CERT = CONFIG[:ssl_cert] && File.exist?(CONFIG[:ssl_cert]) ? CONFIG[:ssl_cert] : nil
SSL_PKEY = CONFIG[:ssl_pkey] && File.exist?(CONFIG[:ssl_pkey]) ? CONFIG[:ssl_pkey] : nil

module Sinatra
  class Application
    def self.run!
      server_options = { Host: bind, Port: port }
      if SSL_CERT && SSL_PKEY
        server_options.merge!(
          SSLEnable: true,
          SSLCertificate: OpenSSL::X509::Certificate.new(File.open(SSL_CERT).read),
          SSLPrivateKey: OpenSSL::PKey::RSA.new(File.open(SSL_PKEY).read) # Could add privKey password here
        )
      end

      Rack::Handler::WEBrick.run self, server_options do |server|
        [:INT, :TERM].each { |sig| trap(sig) { server.stop } }
        server.threaded = settings.threaded if server.respond_to? :threaded=
        set :running, true
      end
    end
  end
end

unless SSL_CERT && SSL_PKEY
  puts '##################################### WARNING ######################################'
  puts 'No SSL certificate and privKey found or selected. Server starting without SSL.'
  puts 'No worries! This is fine. But if you wanted to you could generate one by running:'
  puts '> openssl req -x509 -nodes -days 365 -newkey rsa:1024 -keyout pkey.pem -out cert.crt'
  puts 'Or point to your existing cert and pkey in the config.'
  puts '####################################################################################'
  CONFIG[:public_url].gsub!('https:', 'http:')
end

set :port, CONFIG[:port] || 9494
set :bind, CONFIG[:bind] || '0.0.0.0'

def random_friendly_id
  adjectives = %w(amazing amber beige big black blue brown cheeky cool cosmic cyan dirty dotted easy fancy fat fresh fun genius ginger
                  gold good green grey indigo kind lime loud magenta mighty olive orange peach perky pink purple quick red rose shiny
                  short silver skinny sleek sleepy slow stinky sweet tall teal white yellow).shuffle
  nouns = %w(ant bat bee bird bug bull cat clam cow dog duck eel elk fish fly fox gnu hog mole owl pig ram rat snake wolf worm yak).shuffle
  digits = 3.times.map{rand(0..9)}.join
  "#{adjectives.pop}-#{adjectives.pop}-#{nouns.pop}-#{digits}"
end

before do
  SESSIONS.delete_if {|k, v| v[:expire] < Time.now.to_i }
end

#############################################################################################
# Endpoints
#############################################################################################

get '/' do
  send_file File.join(settings.public_folder, 'index.html')
end

get '/dynamic.js.php' do
  content_type :js
  <<-EOS
  window.TILE_URI = '#{CONFIG[:map_tile_uri]}';
  window.ATTRIBUTION = '#{CONFIG[:map_attribution]}';
  window.DEFAULT_ZOOM = #{CONFIG[:default_zoom]};
  window.MAX_ZOOM = #{CONFIG[:max_zoom]};
  window.MAX_POINTS = #{CONFIG[:max_shown_pts]};
  window.VELOCITY_DELTA_TIME = #{CONFIG[:v_data_points]};
  window.TRAIL_COLOR = '#{CONFIG[:trail_color]}';
  window.VELOCITY_UNIT = #{CONFIG[:velocity_unit].to_json};
  EOS
end

# Handles session initiation for Hauk clients.
# Creates a session ID and associated share URL, and returns these to the client.
# Test: curl -d 'pwd=password&dur=600&int=1' localhost:9494/api/create.php
post '/api/create.php' do
  [:pwd, :dur, :int].each { |e| halt(400, "Missing data\n") if !params[e] }
  halt(403, "Incorrect password!\n") if params[:pwd] != CONFIG[:password]
  duration = params[:dur].to_i
  interval = params[:int].to_f
  halt(400, "Share period is too long!\n") if duration > CONFIG[:max_duration]
  halt(400, "Ping interval is too long!\n") if interval > CONFIG[:max_duration]
  halt(400, "Ping interval is too short!\n") if interval < CONFIG[:min_interval]
  loop while SESSIONS[(fid = random_friendly_id; sid = Digest::MD5.hexdigest("#{fid}#{CONFIG[:salt]}").to_sym)]
  SESSIONS[sid] = {
    expire: Time.now.to_i + duration,
    interval: interval,
    fid: fid,
    locations: []
  }
  "OK\n#{sid}\n#{CONFIG[:public_url]}?#{fid}\n"
end

# Called by the client to receive location updates. A link ID is required to retrieve data.
# Test: curl localhost:9494/api/fetch.php?id=xxxxxx
get '/api/fetch.php' do
  fid = params[:id]
  sid = Digest::MD5.hexdigest("#{fid}#{CONFIG[:salt]}")
  if SESSIONS[sid.to_sym]
    content_type :json
    {
      i: SESSIONS[sid.to_sym][:interval],
      x: SESSIONS[sid.to_sym][:expire],
      l: SESSIONS[sid.to_sym][:locations]
    }.to_json
  else
    halt(404, "Invalid session!\n")
  end
end

# Called from the Hauk app to push location updates to the server.
# Each update contains a location and timestamp from when the location was fetched by the client.
# Test: curl -d 'lat=52.52&lon=13.40&time=1567602256.994&sid=xxxxxx' localhost:9494/api/post.php
post '/api/post.php' do
  [:lat, :lon, :time, :sid].each { |e| halt(400, "Missing data\n") if !params[e] }
  lat = params[:lat].to_f
  lon = params[:lon].to_f
  time = params[:time].to_f
  halt(400, "Invalid location!\n") if lat < -90 || lat > 90 || lon < -180 || lon > 180
  # Not all devices report speed and accuracy, but if available, report them too.
  speed = params[:spd] ? params[:spd].to_f : nil
  accuracy = params[:acc] ? params[:acc].to_f : nil
  sid = params[:sid]
  if SESSIONS[sid.to_sym]
    SESSIONS[sid.to_sym][:locations] << [lat, lon, time, accuracy, speed]
    SESSIONS[sid.to_sym][:locations].shift(SESSIONS[sid.to_sym][:locations].size - CONFIG[:max_cached_pts]) if SESSIONS[sid.to_sym][:locations].size > CONFIG[:max_cached_pts]
    "OK\n"
  else
    halt(404, "Session expired!\n")
  end
end

# Handles session cancellation for Hauk clients.
# Test:curl -d 'sid=xxxxxx' localhost:9494/api/stop.php
post '/api/stop.php' do
  halt(400, "Missing data\n") if !params[:sid]
  SESSIONS.delete(params[:sid].to_sym)
  "OK\n";
end
