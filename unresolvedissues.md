# Unresolved Issues

## Missing users

If a student is removed (disabled?) from the module before it is archived 
the membership record may remain in the membership resource but with a user 
ID that has no record in the users resource.
That means elements might be marked up with bbcarv:uid and bbcarv:mid but
there is bbcarv:sid="" because that is expected to be in the users resource.

At present these elements are not recognised as belonging to a student and
end up in every student's redacted file.  Needs change so element is only
recognised as for everyone if the attribute is absent, not just blank.