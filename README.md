''This document is intended for guidance only and the functionality described in no way represents a binding contract. Details are subject to alteration and correction without notice.''

uaJSON presents a simple HTTP interface for reading and posting messages, wholists and other functions.

All content is in JSON format (Content-Type: application/json). Requests must be authenticated with HTTP BASIC unless otherwise specified.

* Successful calls will get a 200 status code
* Missing authentication will get get a 401
* Requests for non-existent resources will get a 404
* Anything else will get a 500

'''NB: Only 200 responses are guaranteed to contain JSON.''' If you get a different status code check the status message for details e.g. HTTP/1.1 404 Folder 'NotThere' does not exist

=== Some conventions ===

'''JSON uses case sensitive keys, onetwothree is NOT the same as !OneTwoThree'''

Where a value appears in quotes it represents a string. Where a value does not appear in quotes it represents a number or a boolean (true|false).

Users and folders are keyed by lower-cased name with non-alphanums converted to underscores. Data sent to the server must be in this format. Names and folders can be displayed in whatever mixed-case is required.

{{{
POST /folder/private

{ to:"techno", "body":"bingle" }
}}}

will be seen by Techno in Private but creating a new folder called "private" is not possible.

epoch is the number of seconds elapsed since midnight Coordinated Universal Time (UTC) of January 1, 1970, not counting leap seconds.

----

== GET /folders ==

All folders you can access.

You receive a list:

{{{
[
  { "folder":"General", "unread":1, "count":6, "subscribed":true },
  { "folder":"UA", "count":5, "subscribed":false },
  { "folder":"New-Confs", "unread":3, "count":4, "subscribed":true },
  ...
]
}}}

Count is all the non-expired messages in a folder. Count and Unread will always be present, even if 0.

== GET /folders/subscribed

Identical to /folders but restricted to subscribed folders.

== GET /folders/unread

Identical to /folders but restricted to subscribed folders with unread messages.

You can remove the implied filter by using GET /folders/all/unread.

----

== POST /folder/XYZ/subscribe ==

Subscribe to folder XYZ.

You send:

{{{
{ "folder":"A" }
}}}

You receive:

{{{
{ "folder":"A" }
}}}

== POST /folder/XYZ/unsubscribe ==

Unsubscribe from folder XYZ.

You send:

{{{
{ "folder":"A" }
}}}

You receive:

{{{
{ "folder":"A" }
}}}

----

== GET /folder/XYZ ==

All messages in folder XYZ without bodies. 

You receive a list:

{{{
[
  { "folder":"UA-Dev", "id":2000874, "epoch":1289914330, "from":"Isvara", "subject":"DNS", "read":true, "thread": 2324 },
  { "folder":"UA-Dev", "id":2000881, "epoch":1289914759, "from":"BW", "to":"Isvara", "subject":"DNS", "inReplyTo":2000874, "thread": 2324 },
  { "folder":"UA-Dev", "id":2000887, "epoch":1289914963, "from":"isoma", "to":"BW", "subject":"DNS", "inReplyTo":2000881, "thread": 2324 },
  ...
]
}}}

inReplyTo contains the message ID of the immediate parent.

== GET /folder/XYZ/unread ==

Unread messages in folder XYZ without bodies. Same format as /folder/XYZ.

== GET /folder/XYZ/full

All non-expired messages in folder XYZ with bodies. Same format as /folder/XYZ plus:

{{{
  { "folder":"UA-Dev", ... , "body":"Blah blah blah" },
  ...
}}}

----

== GET /messages/saved (since EDF server 2.9 / JSON 0.9) ==

All saved messages in all folders. Same format as /folder/XYZ.

== GET /messages/saved/full (since EDF server 2.9 / JSON 0.9) ==

As above but includes message body.

== GET /thread/123 (since server 2.9 / uaJSON 0.9) ==

All messages in a thread, including cross folder replies. Same format as /folder/XYZ.

== GET /thread/123/full (since server 2.9 / uaJSON 0.9) ==

As above but includes message body.

== GET /message/XYZ ==

Get single message XYZ.

You receive:

{{{
{
  "body": "Shame. Especially as Lovefilm want 9.99/month for unlimited streaming - and\nthey're not built in to my Apple TV (although they are on my Sony BD player).",
  "epoch": 1326140883,
  "folder": "Media",
  "from": "insanity",
  "id": 2124757,
  "inReplyTo": 2124755,
  "inReplyToHierarchy": [
    {
      "from": "Isvara",
      "id": 2124755
    },
    {
      "from": "insanity",
      "id": 2124753
    },
    {
      "from": "Isvara",
      "id": 2124669
    }
  ],
  "position": 83,
  "read": true,
  "replyToBy": [
    {
      "from": "Isvara",
      "id": 2124765
    },
    {
      "from": "rjp",
      "id": 2124775
    }
  ],
  "subject": "netflix.co.uk",
  "to": "Isvara"
}
}}}

A message may also be a poll (since uaJSON 0.9):

{{{
{
  "folder": "Polls",
  ...
  "votes": {
    "votetype": 522,
    vote [
      {
        "id": 1,
        "text": "House, 100 tonnes",
        "numvotes": 2
      },
      {
        "id": 2,
        "text": "Half a house, 100 tonnes",
        "numvotes": 2
      }
    ]
  }
}
}}}

----

== POST /folder/XYZ ==

Create a message in folder XYZ.

You send:

{{{
{ "subject":"foo", "to":"techno", "body":"bar baa" }
}}}

You receive:

{{{
{ "id":"12345", "folder":"test1", "epoch":"1234567890" }
}}}

== POST /message/XYZ ==

Create a message in reply to message XYZ. 

You send, minimally:

{{{
{ "body":"B" }
}}}

Or maximally:

{{{
{ "folder":"test1", "subject":"foo", "to":"techno", "body":"bar baa" }
}}}

You receive:

{{{
{ "id":"12346", "folder":"test1", "epoch":"1234567890", "thread":"23456" }
}}}

to and subject default to the ones in XYZ. Attempts to change thread will be ignored.

== POST /message/read ==

Mark message(s) as read.

You send a list:

{{{
[ 123, 456, 789 ]
}}}

You receive:

{{{
{ "count":3 }
}}}

count is the number of messages marked as read.
*(what about "stay caught up"? - techno)*

== POST /message/unread ==

Remove read mark from message(s). Same format as /message/read.

== POST /message/save (since EDF server 2.9 / JSON 0.9) ==

Mark message(s) as saved. Same format as /message/read.

== POST /message/unsave (since EDF server 2.9 / JSON 0.9) ==

Remove save mark from message(s). Same format as /message/read.

----

== GET /system

Details about the system. It will not include a banner.

You recieve:

{{{
{ epoch:1289914330 }
}}}

Epoch is the current server time. ''This request works without logging in''

----

== GET /user ==

Your own details. You recieve:

{{{
{
  "user":"Techno"
}
}}}

== GET /users ==

All users. You receive a list:

{{{
[
  { "user":"Techno" },
  { "user":"rjp" },
  ...
]
}}}

== GET /users/online ==

You receive a list:

{{{
[
  { "user":"Techno", "epoch":1289914330, "hostname":"one.two.com", "location":"The Two Place" },
  { "user":"rjp", "epoch":1289914330, "hostname":"four.two.com", "location":"The Two Place" },
  ...
]
}}}

epoch is the time at which the most recent request was made.