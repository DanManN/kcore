#!/usr/bin/env bash
while IFS="" read -r p || [ -n "$p" ]
do
	x=($p)
	if [ ${x[0]} -ne ${x[1]} ]; then
		printf '%s\n' "$p"
	fi
done
