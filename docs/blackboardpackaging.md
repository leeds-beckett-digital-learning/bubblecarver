# Blackboard Course Archive Packaging Analysis


## Directory Structure of Course Archive

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

### Student submitted files...

These are handled very differently. They are put into an IMS resource
for this specific purpose.



## Resources - Types of Interest

| type | notes | keep | redact |
|:-----------------------------|:-------------------------------|:----:|:----:|
| assessment/x-bb-qti-attempt | Test responses, feedback and marks. One file per deployed test. (Does this include survey data?) Actual resource is not XML but referenced files are XML. |	Y	| Y |
| assessment/x-bb-qti-pool | Pool questions for tests and surveys. Think questions used in tests are copied into deployed tests rather than referenced.	N	
| assessment/x-bb-qti-survey | Survey questions | N | 
| assessment/x-bb-qti-test | Test questions | Y | N
| course/x-bb-attemptembeddedfiles | Files embedded into assignment responses. One per assignment. | Y | Y
| course/x-bb-attemptfiles | Files attached to assignment responses. One per assignment | Y | Y
| course/x-bb-attemptreceipts | No need to archive | N | 
| course/x-bb-availability-rule | Content adaptive release rules Needed for achievements because this is a record of how student is recorded as having achieved that. Keep every rule file that applies to a document that is also being retained. | Y/N | N
| course/x-bb-availgrouptool | Indicates what tools are made available in which group. One per course. | N | 
| course/x-bb-category | Categories of assessment. (Used by  x-bb-itemcategory) | N | 
| course/x-bb-contenthandler | Copy of system config for building blocks. Could help identify/describe embedded content in student assignment attempt. | Y | N
| course/x-bb-courseappsetting | Availability of tools | N | 
| course/x-bb-courseassessment | One per test. Deployment settings of a test. Inc. time limit. | Y | N
| course/x-bb-courseassessmentcreationsettings | Default settings for new questions in each test. | N | 
| course/x-bb-coursemessage | Messages (emails) sent. Might evidence support. | Y | Y
| course/x-bb-coursemessagefolder | Defines ‘inbox’ and ‘sent’ | Y | 
| course/x-bb-coursemodulepages | Configuration of tools page – original view modules. | N | 
| course/x-bb-coursenavsetting | Enabled tools | N | 
| course/x-bb-coursetoc | Item in menu of content | N | 
| course/x-bb-crsrubricassocation [sic] | Matching rubrics with assessments. | Y | 
| course/x-bb-crsrubriceval | Student marks and feedback broken down by rubric cells? | Y | Y
| course/x-bb-csresourcelink | Links from resources into Xythos?? | N | 
| course/x-bb-gradebook | One per course | Y | Y
| course/x-bb-group | List of groups | Y | 
| course/x-bb-groupattemptfiles | Files embedded in group assessment attempts. Redaction – each student keeps copies of files uploaded by fellow group members but not other groups. | Y | Y
| course/x-bb-groupcontent | Puts groups into student visible group menu item. | N | 
| course/x-bb-groupupload | Files uploaded to groups. One per course covers all groups. Redaction – include all files uploaded by student and student’s fellow group members. | Y | Y
| course/x-bb-itemcategory | Puts test items into categories | N | 
| course/x-bb-review-status | Status of review of content by students. Can be used in availability rules to open access to content for students who have marked content as reviewed. | Y | Y
| course/x-bb-rubrics | No student data – the rubric framework. May be needed for displaying student data | Y | N
| course/x-bb-stdsalignments | Related to ‘goals’. No student data – concerned with auditing course content. | N | 
| course/x-bb-subdocassociation | Related to ‘goals’. No student data – concerned with auditing course content. | N | 
| course/x-bb-trackingevent | Not well formed XML but could be concatenated, topped and tailed to make XML. Text node seems to contain pipe delimited data from database table “activity_accumulator”. After splitting by pipe one column is probably a user ID so it might be possible to redact by including only text that contains “pipe_1234_1pipe” where “_1234_1” is the ID of the current student. | N | 
| course/x-bb-user | Copies of system wide data for users in this module. Redact out other users and also redact unnecessary data of the selected student, e.g. address. | Y | Y
| course/x-bb-usertestoption | Exceptional time limits, number of attempts etc. on tests for individuals and groups. | Y | Y
| discussionboard/x-bb-userforumsettings | User’s preferences for sorting etc. in discussion boards. | N | 
| discussionboard/x-bb-usermsgstate | Record of users viewing messages and other flags. May indicate student having read support message. | Y | Y
| membership/x-bb-coursemembership | Membership data for example role in module | Y | Y
| membership/x-bb-groupmembership | Membership of groups | Y | Y
| resource/x-bb-achievements | Each file records that a student was notified of an achievement. Says little about the achievement. Two other resources are significant – 1) resource/x-bb-document with contenthandler = resource/x-bb-achievement. Defines presentation of the achievement. 2) course/x-bb-availability-rule which defines how a student accesses the achievement. | Y | Y
| resource/x-bb-announcement | One per announcement | N | 
| resource/x-bb-attendance | Type of assessment – tutor record of attendance. | Y | Y
| resource/x-bb-blog | May be assessed and may be group activity | Y | Y
| resource/x-bb-calendar | Module wide calendar entries | N | 
| resource/x-bb-calendar-group | Adds group access rules to calendar entries. | N | 
| resource/x-bb-conference | Top level structure that contains fora. | N | 
| resource/x-bb-discussionboard | AKA Fora. Threads and messages nested within. | N | 
| resource/x-bb-document | Course content. Mostly unimportant for student archive except certain types of document. Subdefine by content handler. Retain: resource/x-bb-achievement (content handler not content type: sighs) | Y/N | N
| resource/x-bb-link | Course content | N | 
| resource/x-bb-retention | Although one type, the root element may vary giving rise to sub-types. NOTIFICATIONRULES describes methods in place for generating student notifications. NOTIFICATIONNOTES records notifications sent to specific students. EWSCOURSEUSERSS records which students are being monitored. To be safe any other base elements should be redacted entirely. | Y | Y
| resource/x-bb-staffinfo | Additional info about staff | N | 
| resource/x-bb-task | Task definitions | Y | 
| resource/x-bb-taskprogress | Per student data regarding students. | Y | Y
| resource/x-bb-video-integration | Problematic. These are links to video material which is stored in cloudflare and linked with grade centre feedback. However, the actual video is not archived and there is no published policy on how long the video will be retained in cloudflare. There are no official mechanisms for downloading video but the streaming server uses simple HTTP GET with range request header so a hack might be possible for downloading this content. | Y | Y
| resource/x-bb-wiki | May be assessed and may be group activity. | Y | Y
| resource/x-osc-basiclti | Not XML. Rarely appears. Obsolete? Relates to a building block that enabled LTI objects. | N | 
| resource/x-plugin-scormengine | Records student activity within a scorm resource. | Y | Y

## Document Resource Content Handlers (SubType) of Interest

| handler | notes | keep | redact |
|:-----------------------------|:-------------------------------|:----:|:----:|
| hyperlink/coursecast | Panopto building block link to video item | N | 
| resource/bb-panopto-bc-mashup | Panopto building block link to video item | N | 
| resource/x-bb-achievement | Represents an awarded ‘acheivement’, e.g. a badge or certificate. Who should be awarded the achievement is determined by a conditional release rule which will reference this resource as its target. Also, when a student is actually awarded a resource of type resource/x-bb-achievements is also created. Redaction – if the student has a resource/x-bb-achievements item with their userId in then also include this resource that describes it. | Y | Y
| resource/x-bb-asmt-survey-link | Link to survey from content area. Wanted for description/body? | N | 
| resource/x-bb-asmt-test-link | Link to test from content area. Wanted for description/body? | N | 
| resource/x-bb-assignment | Link to assignment from content area. Wanted for description/body? | N | 
| resource/x-bb-bloglink | Link to blog (actual blog stored elsewhere) | N | 
| resource/x-bb-blti-link | Link to an LTI tool. Might be interesting to break down statistics based on the link URL and/or placement details. There is potential that the tool might contain assessment related data but this linking document doesn’t have it.  The tool might not be reporting grades etc. back to the grade centre. So, it might be useful to know that these exist in a module the student is enrolled on.  We might need a list of placements to mention and those that need not be – e.g. a reading list definitely isn’t used for assessment but a Panopto link might. | Y | Y
| resource/x-bb-bltiplacement-inspera |  | N | 
| resource/x-bb-bltiplacement-PollEv |  | N | 
| x-bb-bltiplacement-TALIS-ITEM-MENU |  | N | 
| resource/x-bb-courselink | Link to Xythos file | N | 
| resource/x-bb-document | Generic course content |  | 
| resource/x-bb-externallink | Link to any URL | N | 
| resource/x-bb-file | File attachments | N | 
| resource/x-bb-flickr-mashup | Old building block | N | 
| resource/x-bb-folder | Folder to contain other resources | N | 
| resource/x-bb-forumlink | Link to – data elsewhere. | N | 
| resource/x-bb-grouplink | Link to – data elsewhere. | N | 
| resource/x-bb-image | Content that consists of an image | N | 
| resource/x-bb-journallink | Link to – data elsewhere. | N | 
| resource/x-bb-lesson | Learning object version of folder | N | 
| resource/x-bb-lesson-plan | Lesson plan – describes learning | N | 
| resource/x-bb-module-page | ??? What is this ??? | ? | 
| resource/x-bbpi-selfpeer-type1 | Old building block | N | 
| resource/x-bb-qwickly-assignment | Old building block. There were archived after the building block stopped working so actual assessed student content is missing. | N | 
| resource/x-bb-slideshare-mashup | Old building block |  | 
| resource/x-bb-syllabus | Obsolete built in tool | N | 
| resource/x-bb-textbook | Obsolete – replaced by LTI reading list | N | 
| resource/x-bb-textbook-manual | Obsolete – replaced by LTI reading list | N | 
| resource/x-bb-toollink | Original only – link to ‘course tool’ | N | 
| resource/x-bb-video | Link to video content, e.g. youtube? | N | 
| resource/x-bb-wiki | Link wiki – actual wiki is elsewhere | N | 
| resource/x-bb-youtube-mashup | Not even remotely a ‘mashup’. Old building that links to Youtube video | N | 
| resource/x-osc-basiclti | LTI link that is not via a placement – mostly links to pebblepad login? | N | 
| resource/x-plugin-scormengine | Scorm package that could involve storing grades/activity into grade centre. Do we need to know about the content of the package itseld? Probably too complicated to extract. | N | 
| resource/x-turnitin-assignment | The building block version of tii. Actual building block data is not in our archives because the BB was not active when archived. Also this is very old version of building block. | Y | N
| resource/x-turnitintool-assignment | A newer version of the building block. The body text in HTML format may reveal the assign_id and help with making a link to archived Turnitin data. | Y | N
| resource/x-uwe-modfeed | Obsolete building block for surveying | N | 
| resource/x-wimba | Obsolete building block for sync communication | N |  |
