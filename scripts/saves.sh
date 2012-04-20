#!/bin/bash

THISDIR=`dirname $0`
. $THISDIR/uajson.env

uajson "/messages/saved/full"

exit $?
