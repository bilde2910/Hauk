#!/bin/bash

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
    echo -e "\033[91m-c\t\033[0mInstall config file to /etc/hauk"
    echo -e "\033[91m-f\t\033[0mOverwrite without asking"
    exit 1
}

# Print help if no arguments given
if [ "$#" -eq 0 ]; then
    hauk_help
fi

# Get the installation path (last argument)
for webroot; do true; done

# If a flag is the last argument, no path is given, so print help and exit
if [ "$webroot" == "-f" ] || [ "$webroot" == "-c" ]; then
    hauk_help
fi

# Centralized configuration
confdir=/etc/hauk
config=/etc/hauk/config.php
useconf=0

hauk_config() {
    if [ -f "$config" ]; then
        if [ "$1" != "-f" ] && [ "$2" != "-f" ]; then
            read -e -p "Config file already exists! Overwrite? [y/N]: " repl
            if [[ "$repl" == [Yy]* ]]; then
                rm "$config" >/dev/null 2>&1
                if [ -f "$config" ]; then
                    echo "You do not have permissions to install the configuration file in"
                    echo "/etc/hauk. Please run this script as root."
                    exit 5
                fi
            fi
        else
            rm "$config" >/dev/null 2>&1
            if [ -f "$config" ]; then
                echo "You do not have permissions to install the configuration file in"
                echo "/etc/hauk. Please run this script as root."
                exit 5
            fi
        fi
    fi
    if ! [ -f "$config" ]; then
        if ! [ -d "$confdir" ]; then
            mkdir -p "$confdir" >/dev/null 2>&1
        fi
        cp backend-php/include/config.php "$config" >/dev/null 2>&1
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
        read -e -p "Target directory does not exist. Create it? [Y/n]: " empty
        if [[ "$empty" == [Nn]* ]]; then
            echo "Aborting..."
            exit 2
        else
            mkdir -p "$webroot" >/dev/null 2>&1
        fi
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
        read -e -p "Delete files and continue install? [y/N]: " empty
        if [[ "$empty" == [Yy]* ]]; then
            rm -rf "$webroot"
            mkdir "$webroot"
        else
            echo "Aborting..."
            exit 4
        fi
    else
        rm -rf "$webroot"
        mkdir "$webroot"
    fi
fi

if [ "$1" != "-c" ] && [ "$2" != "-c" ]; then
    read -e -p "Install config file to /etc/hauk? [Y/n]: " empty
    if ! [[ "$empty" == [Nn]* ]]; then
        hauk_config
    fi
else
    hauk_config
fi

cp -R backend-php/* "$webroot"
cp -R frontend/* "$webroot"

echo ""
echo -e "\033[1m\033[92mInstallation complete!\033[0m"
echo "Before you use Hauk, make sure to change Hauk's configuration."
echo "The configuration file can be found at:"

# Determine the path in which config is saved
if [ "$useconf" -eq 1 ]; then
    confpath=/etc/hauk/config.php
else
    confpath="$webroot/include/config.php"
fi

echo "$confpath"

# Try to get the user's editor
if [ "$EDITOR" ]; then
    editor="$EDITOR"
elif [ "$SUDO_USER" ]; then
    editor=$(su - $SUDO_USER -c '. ~/.profile; echo $EDITOR')
fi

# If found, prompt to edit the config
if [ "$editor" ]; then
    echo ""
    read -e -p "Do you wish to open this file for editing now? [Y/n]: " edit
    if ! [[ "$edit" == [Nn]* ]]; then
        "$editor" "$confpath"
    fi
fi
