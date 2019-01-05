#!/usr/bin/env bash
while IFS="" read -r p || [ -n "$p" ]
do
	x=($p)
	printf '%s\t%s\n%s\t%s\n' "${x[0]}" "${x[1]}" "${x[1]}" "${x[0]}"
done <$1 | tr -d '\r'
