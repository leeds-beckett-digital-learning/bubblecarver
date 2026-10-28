# Bubble Carver
## (BBL Course Archive Processor)

Tool for processing BlackBoard Learn Course Archives in bulk. The intention is
to discard data relating to course content retaining only data relevent to
students' assessment. Must include submitted work, feedback and marks.

The data is reorganised from course based to student based. In other words from
one archive zip or directory per course to one zip or directory per student.
Will reduce the volume of data retained and arrange it so that the correct
retention schedule can be applied.

Side note : retention of assessment data is based on date that student left
the university.  Different students who studied a particular course module
may leave in different years so deleting data by course is inappropriate.

## The Blackboard Archive Contents

Blackboard archives are extensions of the IMS packaging specification. Almost
all data (other than uploaded binary files) is derived from the SQL database 
but is archived in XML format files.  This promotes backwards compatibility -
to make it likely that an archive created for an older version of Blackboard
Learn having a different SQL database schema can be restored at least partially
into a newer version of Blackboard Learn.

Within the docs directory of this project is an analysis of the [Blackboard Learn
Archive Package](docs.blackboardpackaging.md) with respect to student
assessment data.

## Outline of Design - Batch Processing

Initially an easy to understand process was applied making using of
XSLT transforms to first generate a single all-encompassing XML data file
representation of the course archive and then use that to build an
XML for each student by stripping out data related to other students.  This
student data is transformed, again with XSLT to an HTML file containing a
report.  In parallel binary files are move/copied to the student directories.

The approach relying on XSLT was memory inefficient because the entire
input document needs to be loaded into memory.  In practice on a modestly
specified server head full exceptions were throws with certain course modules.
Massive amounts of heap processing also made processing slow.

A new approach was taken by using STAX for most XML processing. This streams
data threw the processor without needing to build a DOM representation in
the heap before each transform.  This does make the processing a but more 
complicated because unlike in XSLT it isn't possible to easily combine
data from different parts of the input document.  So this is the new design
in outline:

### Aside - ZIP File Extraction
Processing ZIP files using pure Java code proved to be inefficient of
time, memory and storage.  This is for various reasons but the best 
well used library, Apache Commons Archive spends a long time scanning the
entire ZIP archive before it does any extraction and this is very
slow if only a few files need to extracted.

So Bubble Carver uses the 7zip command line tool for all zip file
processing.  If this tool is asked to extract a small number of files
from the archive it works very fast to find them and is also significantly
quicker at extracting them.  We don't ask it to extract files that are not
wanted for student archives and so archives with many gigabytes of
course materials are processed just as fast as those with no uploaded
file course materials.  (Courses with gigabytes of student uploads
of files for assessment do take much longer to extract.)

A temporary directory is used for extracting parts of the ZIP file
in several steps. After all the stages (except generating the
student report) and before processing the next course archive the 
whole temporary directory and its contents are deleted.

### Prep
The few files that we know for certain will be needed in the 
case of every course archive are extracted.  For example
the imsmanifest.xml file which is the key index for all archives.

### Combiner Pass 1a (Dummy run)

Pumps the imsmanifest.xml file through a STAX processor only to discover
the names of resource files that will need to be extracted.  This produces
an output XML file that is only of interest for debugging. The real
data if interest is a list of files that need to be extracted.

### Combiner Pass 1b

Again pumps the imsmanifest.xml file though a STAX processor but this time
as it encounters each resource element in the manifest it opens the resource
XML file and processes that through a filter specialised to the resource
type and inserts it into the one big XML output file.

A key part of this stage is to build tables of look of data that relate
elements in different branches of even different files to each other. This
would not be necessary with XSLT processing but the STAX processor cannot
access information about one branch of the input while processing another one.
One to one and one to many relationships are built up.  Another type of
indexing is done - building a map of groups and group membership.

### Combiner Pass 2

Uses a STAX filter to take the output and the data maps of stage 1b and 
mark up the XML with attributes according to which data belongs to which
student or group.  It also makes an index of student IDs that are for students
that should be included in the archive.  (For example LBU codes student IDs
distinctly for students of partner organisations so these can be filtered out.
Also, we have 'staff as student' account for testing that can be filtered
out.)

### Splitter

The splitter runs a STAX filter multiple times on the same stage 2 output
as its input.  Each time a different student ID is used as a parameter
and a different output XML file is created in a location specific to the 
student.  The processing is simple - all the sections of XML that are marked
for a different student are removed and also all sections for a group that
the student is not a member of are removed.

Also, uploaded files that belong exclusively to the student are moved from
the temporary unzipped area to the student's archive entry location. 
Moving is preferred because it is usually very fast when the source and
destination are on the same disk drive volume.  Files
that are shared with other group members are copied which make take longer
to process.

This may be the last step in a batch processing session.

### Report building

Only needs the students' redacted data file for input. It assumes the uploaded
binary files are in a logical location - it doesn't bother checking.

This is currently done with XSLT because the redacted data file is small 
enough to easily load into memory in full before each transform.  The
shuffling of data in this process is quite complex and would be very hard to 
implement and maintain without loading all the data even if XSLT were
avoided.

At present the same redacted data file is processed multiple times to 
produce multiple HTML reports. One main report presents the grade centre
'row' for the student. Then a page is created for each 'cell' in the grade
centre row containing all relevant data.

For simplicity of maintenance one XSLT transform is used but with
parameters that indicate the type of report. (Because all reports
share common methods of processing data, e.g. proper formatting of
points records.)

