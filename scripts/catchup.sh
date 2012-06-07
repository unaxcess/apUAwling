#!/bin/bash

if [ $# -lt 1 ] ; then
  echo "Usage: $0 <IDs>"
  exit 1
fi

THISDIR=`dirname $0`
. $THISDIR/uajson.env

IDS=`echo "$*" | sed -e "s/ /,/g"`

uajson "/catchup/message/sticky" "[$IDS]"

exit $?
