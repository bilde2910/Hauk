#!/bin/sh

# Hauk automatic installation script for Linux servers
# Copyright (C) 2019 Marius Lindvall under the Apache 2.0 License
# Source code available freely at https://github.com/bilde2910/Hauk

hauk_help() {
    echo -e "\033[1m\033[94mHauk automatic installer\033[0m"
    echo ""
    echo -e "\033[1mUsage:\033[0m"
    echo -e "./install.sh \033[90m[\033[0moptions\033[90m] \033[0mweb_root"
    echo ""
    echo -e "\033[1mDescription:\033[0m"
    echo "Installs Hauk to the specified web root directory, e.g. /var/www/html"
    echo ""
    echo -e "\033[1mOptions:\033[0m"
    echo -e "\033[91m-c\t\033[0mInstall config file to $config"
    echo -e "\033[91m-f\t\033[0mOverwrite without asking"
    exit 1
}

# Centralized configuration
if [ "X$(uname -s)" = "XFreeBSD" ]; then
    confdir=/usr/local/etc/hauk
    config=/usr/local/etc/hauk/config.php
else
    confdir=/etc/hauk
    config=/etc/hauk/config.php
fi
useconf=0

# Print help if no arguments given
if [ "$#" -eq 0 ]; then
    hauk_help
fi

# Get the installation path (last argument)
for webroot; do true; done

# If a flag is the last argument, no path is given, so print help and exit
if [ "$webroot" = "-f" ] || [ "$webroot" = "-c" ]; then
    hauk_help
fi

hauk_config() {
    if [ -f "$config" ]; then
        if [ "$1" != "-f" ] && [ "$2" != "-f" ]; then
            read -p "Config file already exists! Overwrite? [y/N]: " repl
            case "$repl" in
                [Yy]*)
                    rm "$config" >/dev/null 2>&1
                    if [ -f "$config" ]; then
                        echo "You do not have permissions to install the configuration file in"
                        echo "${confdir}. Please run this script as root."
                        exit 5
                    fi
                    ;;
            esac
        else
            rm "$config" >/dev/null 2>&1
            if [ -f "$config" ]; then
                echo "You do not have permissions to install the configuration file in"
                echo "${confdir}. Please run this script as root."
                exit 5
            fi
        fi
    fi
    if ! [ -f "$config" ]; then
        if ! [ -d "$confdir" ]; then
            mkdir -p "$confdir" >/dev/null 2>&1
        fi
        cp backend-php/include/config-sample.php "$config" >/dev/null 2>&1
    fi
    if ! [ -f "$config" ]; then
        echo "You do not have permissions to install the configuration file in"
        echo "/etc/hauk. Please run this script as root."
        exit 5
    fi
    useconf=1
}

if ! [ -d "$webroot" ]; then
    if [ "$1" != "-f" ] && [ "$2" != "-f" ]; then
        read -p "Target directory does not exist. Create it? [Y/n]: " empty
        case "$empty" in
            [Nn]*)
                echo "Aborting..."
                exit 2
                ;;
            *)
                mkdir -p "$webroot" >/dev/null 2>&1
                ;;
        esac
    else
        mkdir -p "$webroot" >/dev/null 2>&1
    fi
fi

if ! [ -d "$webroot" ] || ! [ -w "$webroot" ]; then
    echo "You do not have sufficient permissions to install Hauk to this"
    echo "directory. Please make sure you have write permission to the"
    echo "installation directory and try again."
    exit 3
fi

if [ "$(ls -A "$webroot")" ]; then
    if [ "$1" != "-f" ] && [ "$2" != "-f" ]; then
        echo "WARNING! Target directory is not empty. If you proceed with the"
        echo "installation, all files in the directory will be deleted."
        read -p "Delete files and continue install? [y/N]: " empty
        case "$empty" in
            [Yy]*)
                rm -rf "$webroot"
                mkdir "$webroot"
                ;;
            *)
                echo "Aborting..."
                exit 4
                ;;
        esac
    else
        rm -rf "$webroot"
        mkdir "$webroot"
    fi
fi

if [ "$1" != "-c" ] && [ "$2" != "-c" ]; then
    read -p "Install config file to /etc/hauk? [Y/n]: " empty
    case "$empty" in
        [Nn]*) ;;
        *) hauk_config;;
    esac
else
    hauk_config
fi

cp -R backend-php/* "$webroot"
cp -R frontend/* "$webroot"

# Determine the path in which config is saved
if [ "$useconf" -eq 0 ]; then
    confpath="$webroot/include/config.php"
    cp backend-php/include/config-sample.php "$confpath" >/dev/null 2>&1
fi

echo ""
echo -e "\033[1m\033[92mInstallation complete!\033[0m"
echo "Before you use Hauk, make sure to change Hauk's configuration."
echo "The configuration file can be found at:"
echo -e "\033[1m$confpath\033[0m"

# Try to get the user's editor
if [ "$EDITOR" ]; then
    editor="$EDITOR"
elif [ "$SUDO_USER" ]; then
    editor=$(su - $SUDO_USER -c '. ~/.profile; echo $EDITOR')
fi

# If found, prompt to edit the config
if [ "$editor" ]; then
    echo ""
    read -p "Do you wish to open this file for editing now? [Y/n]: " edit
    case "$edit" in
        [Nn]*) ;;
        *) "$editor" "$confpath";;
    esac
fi
