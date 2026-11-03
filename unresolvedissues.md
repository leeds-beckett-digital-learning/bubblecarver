# Unresolved Issues

## Missing users

If a student is removed (disabled?) from the module before it is archived 
the membership record may remain in the membership resource but with a user 
ID that has no record in the users resource.
That means elements might be marked up with bbcarv:uid and bbcarv:mid but
there is bbcarv:sid="" because that is expected to be in the users resource.

At present these elements are not recognised as belonging to a student and
end up in every student's redacted file.  Needs change so element is only
recognised as for-everyone if the attribute is absent, not just blank.

## LTI tools referenced from gradebook.

E.g. turnitin dropbox.
At present LTI tool based gradebook columns can be detected from
OUTCOMEDEFINITION/SCORE_PROVIDER_HANDLE/@value = 'resource/x-bb-blti-link'
But nothing in the gradebook resource says what kind of LTI tool that is.
What it does have is a resource ID in 
OUTCOMEDEFINITION/CONTENTID/@value

So, to identify, for example a turnitin drop box it is necessary to
look in the resource/x-bb-document resource file.  That has
an element 
/CONTENT/URL/@value
There are also elements /CONTENT/EXTENDEDDATA/ENTRY
The most useful may be
/CONTENT/EXTENDEDDATA/ENTRY[@key = 'placementHandle']/text()

This is for later development because it requires loading more resource
types from the course archives.