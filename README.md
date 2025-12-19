# lbu-bubblecarver
Tool for processing BlackBoard Learn Course Archives.


## Notes on Archive Format

### /csfiles/
Files that were in the content collection (Xythos)

### /csfiles/home_dir/
Files from the module home directory, i.e. course content

### /csfiles/privatedoc/
Probably from the /internal/ part of Xythos.

### /csfiles/privatedoc/gradebook
Files that markers have uploaded in relation to student submissions, i.e.
attached to feedback etc.

### /csfiles/privatedoc/gradebook/notesAndFeedback
The only subdirectory of the parent?

### /csfiles/privatedoc/gradebook/notesAndFeedback/attempt
Contains subdirectories each named after the attempt ID. Files are referenced
from notes and feedback fields of attempt records in the grading data file.

### /csfiles/privatedoc/gradebook/notesAndFeedback/gradeDetail
Contains subdirectories each named after the outcome ID. Files are referenced
from notes and feedback fields of outcome records. These are overrides of
data in the (potentially) multiple attempts.

### /csfiles/privatedoc/gradebook/notesAndFeedback/gradeHistory
Contains subdirectories each named after the outcome ID. Each of these
contains a subsubdirectory named after the UNIX epoch timestamp of a grade
history event. These contain files referenced in those data records (which
could well be duplicates of files in the attempt folder.

### /csfiles/privatedoc/gradebook/notesAndFeedback/groupAttempt

## Student submitted files...
These are handled very differently. They are put into an IMS resource
for this specific purpose.

