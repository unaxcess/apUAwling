#!/bin/bash

THISDIR=`dirname $0`
. $THISDIR/uajson.env

if [ $# -ne 1 ] ; then
  usage $0 "<ID>"
fi

ID="$1"

uajson "/message/$ID"

exit $?
