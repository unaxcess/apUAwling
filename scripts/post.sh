#!/bin/bash

if [ $# -lt 3 ] ; then
  echo "Usage: $0 <folder> <subject> <body>"
  exit 1
fi

THISDIR=`dirname $0`
. $THISDIR/uajson.env

FOLDER="$1"
SUBJECT="$2"
shift
shift
BODY="$@"

uajson "/folder/$FOLDER" "{\"subject\":\"$SUBJECT\", \"body\":\"$BODY\"}"

exit $?
