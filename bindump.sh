#!/usr/bin/env bash
od -v -A n -t d4 --endian=big "$@"
