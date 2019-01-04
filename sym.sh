#!/usr/bin/env bash
while IFS="" read -r p || [ -n "$p" ]
do
	x=($p)
	printf '%s\n%s\t%s\n' "$p" "${x[1]}" "${x[0]}"
done <$1 | tr -d '\r'
