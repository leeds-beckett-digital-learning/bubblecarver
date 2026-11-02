<?xml version="1.0" encoding="UTF-8"?>

<!--
    Document   : studentreport.xsl
    Created on : 8 January 2026, 10:47
    Author     : maber01
    Description:
        Purpose of transformation follows.
-->

<xsl:stylesheet 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  xmlns:bbcarv="http://www.leedsbeckett.ac.uk/bubblecarver"
  xmlns:ins="http://www.leedsbeckett.ac.uk/bubblecarver/instant"
  xmlns:java="http://xml.apache.org/xalan/java"
  extension-element-prefixes="java"
  version="1.0">
  <xsl:output method="html"/>
  
  <xsl:param name="list-helper-id" select="''"/>
  <xsl:param name="outcome-def-id" select="''"/>
  <xsl:param name="style" select="''"/>

  <xsl:variable name="sid" select="/bbcarv:archive/@bbcarv:sid"/>
  <xsl:variable name="gradebook" select="/bbcarv:archive/manifest/resources/resource[@type='course/x-bb-gradebook']/bbcarv:data/GRADEBOOK"/>
  
  
  <xsl:template match="/">
    <html>
      <head>
        <title>Student Course Report <xsl:value-of select="$sid"/></title>
        <style>
          details { padding-left: 2em; }
		  <xsl:value-of select="$style"/>
        </style>
      </head>
      <body>
        <h1>
          <xsl:value-of select="concat( $sid, ' ', .//USERS/USER/NAMES/FAMILY/@value, ' ', .//USERS/USER/NAMES/GIVEN/@value )"/>
        </h1>
        <h2><xsl:value-of select=".//COURSE/TITLE/@value"/></h2>
        <h3>Course ID: <xsl:value-of select=".//COURSE/COURSEID/@value"/></h3>
        <p>[<xsl:value-of select="$outcome-def-id"/>]</p>
        <xsl:choose>
          <xsl:when test="string-length( $outcome-def-id ) = 0">
            <xsl:apply-templates select="$gradebook/OUTCOMEDEFINITIONS" mode="alloutcomedef"/>            
          </xsl:when>
          <xsl:otherwise>
            <xsl:apply-templates select="$gradebook/OUTCOMEDEFINITIONS" mode="oneoutcomedef"/>
          </xsl:otherwise>
        </xsl:choose>
        
      </body>
    </html>
  </xsl:template>

  <!-- match="OUTCOMEDEFINITIONS" mode="alloutcomedef" -->
  <xsl:template match="OUTCOMEDEFINITIONS" mode="alloutcomedef">
    <table>
      <thead>
        <tr>
          <th>Title</th>
          <th>Type</th>
          <th style="width: 5em">Points Avail.</th>
          <th style="width: 5em">Points</th>
          <th style="width: 5em">Grade</th>
          <th style="width: 3em">Attempts</th>
          <th>Detail</th>
        </tr>
      </thead>
      <tbody>
        <xsl:apply-templates select="OUTCOMEDEFINITION" mode="alloutcomedef"/>
      </tbody>
    </table>
  </xsl:template>

  <!-- match="OUTCOMEDEFINITIONS" mode="oneoutcomedef" -->
  <xsl:template match="OUTCOMEDEFINITIONS" mode="oneoutcomedef">
    <xsl:apply-templates select="OUTCOMEDEFINITION[@id = $outcome-def-id]" mode="oneoutcomedef"/>
  </xsl:template>

  <!-- match="OUTCOMEDEFINITION" mode="type" -->
  <xsl:template match="OUTCOMEDEFINITION" mode="type">
    <xsl:variable name="url" select="bbcarv:document/bbcarv:URL/@value"/>
    <xsl:if test="count( GROUPATTEMPTS/GROUPATTEMPT ) &gt; 0">Group </xsl:if>
    <xsl:choose>
      <xsl:when test="'true' = ISCALCULATED/@value">Calculated: <xsl:value-of select="CALCULATIONTYPE/@value"/></xsl:when>
      <xsl:when test="string-length( LTI_DOMAIN_ID/@value ) &gt; 0">LTI Tool
        <xsl:if test="string-length( $url ) &gt; 0">
          <br/>(<xsl:value-of select="$url"/>)
          <br/>Score, grade and/or feedback may be on external web site.
        </xsl:if>
      </xsl:when>
      <xsl:when test="SCORE_PROVIDER_HANDLE/@value = 'resource/x-bb-assignment'">Assignment</xsl:when>
      <xsl:when test="SCORE_PROVIDER_HANDLE/@value = 'resource/x-bb-assessment'">Test</xsl:when>
      <xsl:when test="SCORE_PROVIDER_HANDLE/@value = 'resource/x-bb-assessment-survey'">Survey</xsl:when>
      <xsl:when test="SCORE_PROVIDER_HANDLE/@value = 'resource/x-bb-bloglink'">Blog or Journal</xsl:when>
      <xsl:when test="SCORE_PROVIDER_HANDLE/@value = 'resource/x-bb-wiki'">Wiki</xsl:when>
      <xsl:when test="SCORE_PROVIDER_HANDLE/@value = 'resource/x-plugin-scormengine'">SCORM Package</xsl:when>
      <xsl:when test="starts-with(HANDLERURL/@value, '/webapps/ip-turnitintool')">TurnItIn Building Block<br/>(Look for data in TII archive)</xsl:when>
      <xsl:otherwise>Other = <xsl:value-of select="SCORE_PROVIDER_HANDLE/@value"/> <xsl:value-of select="HANDLERURL/@value"/> </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- match="OUTCOMEDEFINITION" mode="alloutcomedef" -->
  <xsl:template match="OUTCOMEDEFINITION" mode="alloutcomedef">
      <xsl:choose>
        <xsl:when test="TITLE/@value = 'OutcomeDefinition.Total.title'">
          <xsl:comment>Outcomedefinition for Total omitted.</xsl:comment>
        </xsl:when>
        <xsl:when test="TITLE/@value = 'OutcomeDefinition.WeightedTotal.title'">
          <xsl:comment>Outcomedefinition for Weighted Total omitted.</xsl:comment>          
        </xsl:when>
        <xsl:otherwise>
          <xsl:apply-templates select="." mode="included"/>
        </xsl:otherwise>
      </xsl:choose>
  </xsl:template>
  
  <!-- match="OUTCOMEDEFINITION" mode="included" -->  
  <xsl:template match="OUTCOMEDEFINITION" mode="included">
    <xsl:variable name="url" select="bbcarv:document/bbcarv:URL/@value"/>
    <xsl:variable name="title" select="TITLE/@value"/>
    
    <tr>
      <th><xsl:value-of select="$title"/></th>
      <td class="type-cell">
        <xsl:apply-templates select="." mode="type"/>
      </td>
      <td><xsl:value-of select="java:uk.ac.leedsbeckett.bubblecarver.xsl.StringProcessor.reformatScore( POINTSPOSSIBLE/@value )"/></td>
      <td>
        <xsl:apply-templates select="OUTCOMES/OUTCOME" mode="points"/>        
      </td>
      <td>
        <xsl:apply-templates select="OUTCOMES/OUTCOME" mode="grade"/>        
      </td>
      <td>
        <xsl:if test="CALCULATIONTYPE/@value = 'NON_CALCULATED'">
          <xsl:choose>
            <xsl:when test="count( OUTCOMES/OUTCOME/ATTEMPTS/ATTEMPT ) = 0">
              <xsl:text>~</xsl:text>
            </xsl:when>
            <xsl:when test="count( OUTCOMES/OUTCOME/ATTEMPTS/ATTEMPT ) = 1">
              <xsl:text>1 Attempt</xsl:text>
            </xsl:when>
            <xsl:otherwise>
              <xsl:value-of select="count( OUTCOMES/OUTCOME/ATTEMPTS/ATTEMPT )"/> Attempts
            </xsl:otherwise>
          </xsl:choose>
        </xsl:if>
      </td>
      <td>
        <xsl:if test="CALCULATIONTYPE/@value = 'NON_CALCULATED' and count( OUTCOMES/OUTCOME/ATTEMPTS/ATTEMPT ) &gt; 0">
		  <!-- only add OUTCOMEDEFINITION to list of detailed reports if we link to it from here. -->
		  <xsl:variable name="odid" 
				 select="java:uk.ac.leedsbeckett.bubblecarver.xsl.GeneralListHelper.add( $list-helper-id, @id )"/>
          <xsl:variable name="href" select="concat( 'report', @id, '.html' )"/>
          <a href="{$href}" target="_blank" rel="noopener">Detail</a>
		  <!-- odid isn't evaluated if it isn't used so add comment with it. -->
		  <xsl:comment>OUTCOMEDEFINITION @id <xsl:value-of select="@id"/> <xsl:value-of select="$odid"/></xsl:comment>
        </xsl:if>        
      </td>
    </tr>
  </xsl:template>

  <!-- match="OUTCOMEDEFINITION" mode="oneoutcomedef" -->  
  <xsl:template match="OUTCOMEDEFINITION" mode="oneoutcomedef">
    <xsl:variable name="title" select="TITLE/@value"/>
    <xsl:variable name="url" select="bbcarv:document/bbcarv:URL/@value"/>
    <h2><xsl:value-of select="$title"/></h2>
    <p>
      <strong>Type: </strong>
      <xsl:apply-templates select="." mode="type"/>
    </p>
    <p>
      <strong>Point Available: </strong>
      <xsl:value-of select="java:uk.ac.leedsbeckett.bubblecarver.xsl.StringProcessor.reformatScore( POINTSPOSSIBLE/@value )"/>
    </p>
    <p>
      <strong>Points: </strong>
      <xsl:apply-templates select="OUTCOMES/OUTCOME" mode="points"/>        
    </p>
    <p>
      <strong>Grade: </strong>
      <xsl:apply-templates select="OUTCOMES/OUTCOME" mode="grade"/>        
    </p>
    <div class="detail">
      <xsl:choose>
        <xsl:when test="count( OUTCOMES/OUTCOME ) = 1">
          <xsl:apply-templates select="OUTCOMES/OUTCOME"/>
        </xsl:when>
        <xsl:otherwise>No outcome data for this item.</xsl:otherwise>
      </xsl:choose>
    </div>
  </xsl:template>

  <!-- match="OUTCOME" mode="points" -->  
  <xsl:template match="OUTCOME" mode="points">
    <xsl:choose>
      <xsl:when test="count( OVERRIDE_SCORE ) = 0 and ../../AGGREGATIONMODEL/@value = 'Last'">
        <xsl:apply-templates select="." mode="points-last-attempt"/>
      </xsl:when>
      <xsl:when test="count( OVERRIDE_SCORE ) = 0">
        Attempt selection: <xsl:value-of select="../../AGGREGATIONMODEL/@value"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="java:uk.ac.leedsbeckett.bubblecarver.xsl.StringProcessor.reformatScore( OVERRIDE_SCORE/text() )"/> (Overrides Attempts)
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- match="OUTCOME" mode="grade" -->  
  <xsl:template match="OUTCOME" mode="grade">
    <xsl:choose>
      <xsl:when test="count( OVERRIDE_GRADE ) = 0 and ../../AGGREGATIONMODEL/@value = 'Last'">
        <xsl:apply-templates select="." mode="grade-last-attempt"/>        
      </xsl:when>
      <xsl:when test="count( OVERRIDE_GRADE ) = 0">
        Attempt selection: <xsl:value-of select="../../AGGREGATIONMODEL/@value"/>
      </xsl:when>
      <xsl:otherwise>
        <xsl:value-of select="OVERRIDE_GRADE/text()"/> (Overrides Attempts)
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="OUTCOME" mode="points-last-attempt">
	<xsl:variable name="completedattempts" select="ATTEMPTS/ATTEMPT[STATUS/@value = 'Completed']"/>
    <xsl:choose>
      <xsl:when test="count( $completedattempts ) = 0">No Completed Attempt</xsl:when>
	  <xsl:otherwise>
        <!-- technique found https://stackoverflow.com/questions/3786443/xpath-to-get-the-element-with-the-highest-id
	         to find most recent attempt with Completed status. Uses the epoch seconds version of the original timestamp attribute -->
        <xsl:variable name="mostrecentcompletedattempt" select="$completedattempts[ not( ../ATTEMPT/DATEATTEMPTED/@ins:value &gt; DATEATTEMPTED/@ins:value ) ]"/>
        <xsl:choose>
          <xsl:when test="count( $mostrecentcompletedattempt ) = 0">Unable to locate last attempt</xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="java:uk.ac.leedsbeckett.bubblecarver.xsl.StringProcessor.reformatScore( $mostrecentcompletedattempt/SCORE/@value )"/>
		  </xsl:otherwise>
        </xsl:choose>
	  </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <xsl:template match="OUTCOME" mode="grade-last-attempt">
	<xsl:variable name="completedattempts" select="ATTEMPTS/ATTEMPT[STATUS/@value = 'Completed']"/>
    <xsl:choose>
      <xsl:when test="count( $completedattempts ) = 0">No Completed Attempt</xsl:when>
	  <xsl:otherwise>
        <!-- technique found https://stackoverflow.com/questions/3786443/xpath-to-get-the-element-with-the-highest-id
	         to find most recent attempt with Completed status. Uses the epoch seconds version of the original timestamp attribute -->
        <xsl:variable name="mostrecentcompletedattempt" select="$completedattempts[ not( ../ATTEMPT/DATEATTEMPTED/@ins:value &gt; DATEATTEMPTED/@ins:value ) ]"/>
        <xsl:choose>
          <xsl:when test="count( $mostrecentcompletedattempt ) = 0">Unable to locate last attempt</xsl:when>
          <xsl:otherwise>
            <xsl:value-of select="$mostrecentcompletedattempt/GRADE/@value"/>
		  </xsl:otherwise>
        </xsl:choose>
	  </xsl:otherwise>
    </xsl:choose>
  </xsl:template>

  <!-- match="OUTCOME" -->  
  <xsl:template match="OUTCOME">
    <xsl:if test="count( FEEDBACK_TO_USER/TEXT ) = 1 or count( INSTRUCTOR_COMMENTS/TEXT ) = 1">
      <h3>Overrides</h3>
      <div>(Override scores, feedback etc. are entered by a marker into the 
        grade centre item regardless of any attempts. This could be because
        work was submitted off-line, because it is based on multiple attempts
        or for some other unusual circumstances.)</div>
    </xsl:if>

    <xsl:if test="count( FEEDBACK_TO_USER/TEXT ) = 1">
      <div class="attempt-comments">
        <h3>Feedback</h3>
        <xsl:apply-templates select="FEEDBACK_TO_USER" mode="override"/>
      </div>
    </xsl:if>
	  
    <xsl:if test="count( INSTRUCTOR_COMMENTS/TEXT ) = 1">
      <div class="attempt-notes">
        <h3>Private Notes</h3>
        <xsl:apply-templates select="INSTRUCTOR_COMMENTS" mode="override"/>
      </div>      
    </xsl:if>
	
	<div class="attempt-list">
      <xsl:if test="count(ATTEMPTS/ATTEMPT) &gt; 1">
        <p>In reverse date order</p>
      </xsl:if>
      <xsl:apply-templates select="ATTEMPTS/ATTEMPT">
	    <!-- Sorting by date can be problematic due to low precision of dates -->
	    <!-- IDs are created unique and in ascending order -->
        <xsl:sort select="@id" order="descending"/>
      </xsl:apply-templates>
	</div>
  </xsl:template>

  <!-- match="ATTEMPT" -->  
  <xsl:template match="ATTEMPT">
    <xsl:variable name="groupattemptid" select="string(GROUP_ATTEMPT_ID/@value)"/>

	<!--
    <p>groupattemptid = <xsl:value-of select="$groupattemptid"/></p>
	-->
	
    <details class="attempt">
	  <xsl:if test="position() = 1">
	    <xsl:attribute name="open">true</xsl:attribute>
	  </xsl:if>
      <summary>Attempt <xsl:value-of select="DATEATTEMPTED/@value"/>  Status <xsl:value-of select="STATUS/@value"/></summary>
	  <div class="attempt-content">

	  <h4>Student Work</h4>
	  <p>Please bear in mind that students are sometimes assessed here on
	  work that was submitted in part or in full via some other tool.</p>
	  
	  <xsl:if test="STATUS/@value = 'In Progress'">
	    <p>For this attempt the student entered the tool but did not actually 
		submit work. Any saved work was not visible to markers. (In Progress)</p>
	  </xsl:if>

	  <xsl:apply-templates select="." mode="attemptsubmission"/>
      <xsl:apply-templates select="ancestor::OUTCOMEDEFINITION/GROUPATTEMPTS/GROUPATTEMPT[@id = $groupattemptid]" mode="attemptsubmission"/>

	  <h4>Marking</h4>
      <div>
        <h5>Individual assessment details</h5>
        <xsl:apply-templates select="." mode="attemptcontent"/>
      </div>

      <xsl:if test="string-length($groupattemptid) != 0">
        <p>The individual assessment is based on a group assessment.</p>
        <div>
          <h5>Group assessment details</h5>
          <xsl:apply-templates select="ancestor::OUTCOMEDEFINITION/GROUPATTEMPTS/GROUPATTEMPT[@id = $groupattemptid]" mode="attemptcontent"/>
        </div>
      </xsl:if>

	  </div>
    </details>
  </xsl:template>
  
  <!-- match="*" mode="attemptsubmission" -->
  <xsl:template match="*" mode="attemptsubmission">
    <xsl:variable name="aid" select="@id"/>
    <xsl:variable name="attachedfiles" 
      select="/bbcarv:archive//ATTEMPTFILES/ATTEMPTFILE[ATTEMPTID/@value = $aid] | /bbcarv:archive//GROUPATTEMPTFILES/GROUPATTEMPTFILE[GROUPATTEMPTID/@value = $aid]"/>                
    <xsl:variable name="numfiles" select="count($attachedfiles)"/>
	<xsl:variable name="textlen" select="string-length( STUDENTSUBMISSION/TEXT )"/>
	<xsl:variable name="special">
      <xsl:if test="ancestor::OUTCOMEDEFINITION/SCORE_PROVIDER_HANDLE/@value = 'resource/x-bb-wiki'">
        <xsl:variable name="wiki" select="ancestor::resources/resource[@type = 'resource/x-bb-wiki']/bbcarv:data/wiki[groupAttemptID/text() = $aid]"/>
        <xsl:apply-templates select="$wiki" mode="textonly"/>
      </xsl:if>      
	</xsl:variable>
	<xsl:variable name="speciallen" select="string-length( translate( $special,' \t\r\n', '' ) )"/>

	<!--
    <p>id = <xsl:value-of select="$aid"/> numfiles = <xsl:value-of select="$numfiles"/> textlen = <xsl:value-of select="$textlen"/> speciallen = <xsl:value-of select="$speciallen"/></p>
    -->
	
    <xsl:if test="$numfiles &gt; 0 or $textlen &gt; 0 or $speciallen &gt; 0">
      <div class="attempt-submission">
	    <xsl:if test="name() = 'GROUPATTEMPT'">
		  <p>This is a group submission. The student may receive the group mark or a personally adjusted mark.</p>
		</xsl:if>
        <!-- may or may not have text directly in the attempt depending on
        the type of item. -->
        <xsl:if test="$textlen &gt; 0">
          <h3>Textual Submission</h3>
          <div>
            <xsl:apply-templates select="STUDENTSUBMISSION"/>
          </div>
        </xsl:if>
		<!-- include attached files if there are any -->
        <xsl:apply-templates select="." mode="attemptattachments"/>
		<!-- resource specific submission content -->
        <xsl:if test="ancestor::OUTCOMEDEFINITION/SCORE_PROVIDER_HANDLE/@value = 'resource/x-bb-wiki'">
          <xsl:variable name="wiki" select="ancestor::resources/resource[@type = 'resource/x-bb-wiki']/bbcarv:data/wiki[groupAttemptID/text() = $aid]"/>
          <xsl:apply-templates select="$wiki"/>
        </xsl:if>      
      </div>
	</xsl:if>
  </xsl:template> 
  
  
  <!-- match="*" mode="attemptcontent" -->  
  <!-- for ATTEMPT or GROUPATTEMPT -->
  <xsl:template match="*" mode="attemptcontent">
    <xsl:variable name="selectedstageda" select="STAGED_GRADING_ATTEMPTS/STAGED_GRADING_ATTEMPT[STAGED_GRADING_STATUS/@value = 'Completed']"/>
    <xsl:variable name="selectedstagedg" select="STAGED_GRADING_GROUP_ATTEMPTS/STAGED_GRADING_GROUP_ATTEMPT[STAGED_GRADING_STATUS/@value = 'Completed']"/>
	
	<xsl:choose>
	  <xsl:when test="$selectedstagedg">
        <div>
          <h4>Staged Group Grading</h4>
          <xsl:apply-templates select="$selectedstagedg" mode="stagedattempt">
            <xsl:sort select="@id"  order="descending"/>
          </xsl:apply-templates>
        </div>
	  </xsl:when>
	  <xsl:when test="$selectedstageda">
        <div>
          <h4>Staged Grading</h4>
          <xsl:apply-templates select="$selectedstageda" mode="stagedattempt">
            <xsl:sort select="@id"  order="descending"/>
          </xsl:apply-templates>
        </div>	  
	  </xsl:when>
	  <xsl:otherwise>
        <table>
          <tbody>
            <tr>
              <th>First Graded:</th>
              <td><xsl:value-of select="DATEFIRSTGRADEDED/@value"/></td>
            </tr>
            <tr>
              <th>Last Graded:</th>
              <td><xsl:value-of select="DATELASTGRADED/@value"/></td>
            </tr>
            <tr>
              <th>Score:</th>
              <td><xsl:value-of select="java:uk.ac.leedsbeckett.bubblecarver.xsl.StringProcessor.reformatScore( SCORE/@value )"/></td>
            </tr>
            <tr>
              <th>Grade:</th>
              <td><xsl:value-of select="GRADE/@value"/></td>
            </tr>
          </tbody>
        </table>
		
        <xsl:if test="count( INSTRUCTORCOMMENTS/TEXT ) = 1">
          <div class="attempt-comments">
            <h3>Feedback</h3>
            <xsl:apply-templates select="INSTRUCTORCOMMENTS"/>
          </div>
		</xsl:if>
		
        <xsl:if test="count( INSTRUCTORNOTES/TEXT ) = 1">
          <div class="attempt-notes">
            <h3>Marker Private Notes</h3>
            <xsl:apply-templates select="INSTRUCTORNOTES"/>
          </div>
	    </xsl:if>
		
	  </xsl:otherwise>
	</xsl:choose>
  </xsl:template>

  <!-- match="ATTEMPT" mode="attemptattachments" -->  
  <xsl:template match="ATTEMPT" mode="attemptattachments">
    <xsl:variable name="aid" select="@id"/>
    <xsl:variable name="restype" select="'course/x-bb-attemptfiles'"/>
    <xsl:variable name="attachedfiles" 
        select="/bbcarv:archive/manifest/resources/resource[@type=$restype]/bbcarv:data/ATTEMPTFILES/ATTEMPTFILE[ATTEMPTID/@value = $aid]"/>                
    <h3>Attached Files - <xsl:value-of select="count( $attachedfiles )"/></h3>
    <xsl:if test="count( $attachedfiles ) &gt; 0">
      <ul>
        <xsl:apply-templates select="$attachedfiles"/>
      </ul>
    </xsl:if>
  </xsl:template>

  <xsl:template match="GROUPATTEMPT" mode="attemptattachments">
    <xsl:variable name="aid" select="@id"/>
    <xsl:variable name="restype" select="'course/x-bb-groupattemptfiles'"/>
    <xsl:variable name="attachedfiles" 
 select="/bbcarv:archive/manifest/resources/resource[@type=$restype]/bbcarv:data/GROUPATTEMPTFILES/GROUPATTEMPTFILE[GROUPATTEMPTID/@value = $aid]"/>                
    <xsl:if test="count( $attachedfiles ) &gt; 0">
      <h3>Attached Files - <xsl:value-of select="count( $attachedfiles )"/></h3>
      <ul>
        <xsl:apply-templates select="$attachedfiles"/>
      </ul>
    </xsl:if>
  </xsl:template>

  <xsl:template match="*" mode="stagedattempt">
    <xsl:variable name="stagedid" select="@id"/>
    <!-- -->
    <xsl:variable name="rubricevalcount">
      <xsl:choose>
        <xsl:when test="name() = 'STAGED_GRADING_GROUP_ATTEMPT'">
          <xsl:value-of select="count( //RUBRIC_EVALUATION_COLLECTION/RUBRIC_EVALUATION[STAGED_GROUP_ATTEMPT_ID/@value = $stagedid] )"/>
        </xsl:when>
        <xsl:otherwise>
          <xsl:value-of select="count( //RUBRIC_EVALUATION_COLLECTION/RUBRIC_EVALUATION[STAGED_ATTEMPT_ID/@value = $stagedid] )"/>
        </xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
    <xsl:variable name="rubriceval" 
      select="//RUBRIC_EVALUATION_COLLECTION/RUBRIC_EVALUATION[STAGED_GROUP_ATTEMPT_ID/@value = $stagedid] | //RUBRIC_EVALUATION_COLLECTION/RUBRIC_EVALUATION[STAGED_ATTEMPT_ID/@value = $stagedid]"/>
    <!-- -->
    <div style="background: rgb(250,255,250);">
      <table>
        <tr>
          <th>Date</th>
          <td><xsl:value-of select="STAGED_GRADING_DATE_ADDED/@value"/></td>
        </tr>
        <tr>
          <th>Status</th>
          <td><xsl:value-of select="STAGED_GRADING_STATUS/@value"/></td>
        </tr>
        <tr>
          <th>Score</th>
          <td><xsl:value-of select="java:uk.ac.leedsbeckett.bubblecarver.xsl.StringProcessor.reformatScore( STAGED_GRADING_SCORE/@value )"/></td>
        </tr>
        <tr>
          <th>Grade</th>
          <td><xsl:value-of select="STAGED_GRADING_GRADE/@value"/></td>
        </tr>
        <tr>
          <th>Marker Feedback</th>
          <td>
            <xsl:if test="count(STAGED_GRADING_INSTRUCTOR_COMMENT) = 1">
              <xsl:variable name="dir">
                <xsl:choose>
                  <xsl:when test="name() = 'STAGED_GRADING_GROUP_ATTEMPT'">/groupattempt/</xsl:when>
                  <xsl:otherwise>/attempt/</xsl:otherwise>
                </xsl:choose>
              </xsl:variable>
              <xsl:variable name="aid" select="../STAGED_GRADING_RELATED_ATTEMPT_ID/@value"/>
              <xsl:variable name="comment" select="STAGED_GRADING_INSTRUCTOR_COMMENT/@value"/>
              <xsl:variable name="commenttype" select="STAGED_GRADING_FEEDBACK_FORMAT_TYPE/@value"/>
              <xsl:variable name="embeddedbase" 
                select="concat( 'csfiles/privatedoc/gradebook/notesAndFeedback', $dir, $aid, '/', $stagedid, '/' )"/>
              <xsl:call-template name="textoutputgeneral">
                <xsl:with-param name="original" select="$comment"/>
                <xsl:with-param name="type" select="$commenttype"/>
                <xsl:with-param name="base" select="$embeddedbase"/>
                <xsl:with-param name="v" select="'@X@EmbeddedFile.cslocation@X@'"/>
              </xsl:call-template>
            </xsl:if>
          </td>
        </tr>
        <xsl:if test="$rubricevalcount &gt; 0">
          <tr>
            <td colspan="2">
              <xsl:apply-templates select="$rubriceval"/>
            </td>
          </tr>
        </xsl:if>
      </table>
    </div>  
  </xsl:template>
    
  <xsl:template match="ATTEMPTFILE">
    <xsl:variable name="attemptid" select="ATTEMPTID/@value"/>
    <xsl:variable name="relurl" select="concat( ../../../@xml:base, '/', @id, '/', FILE/NAME/text() )"/>
    <li>
      <a href="{$relurl}"><xsl:value-of select="FILE/NAME/text()"/></a>
    </li>
  </xsl:template>

  <xsl:template match="GROUPATTEMPTFILE">
    <xsl:variable name="attemptid" select="GROUPATTEMPTID/@value"/>
    <xsl:variable name="relurl" select="concat( ../../../@xml:base, '/', @id, '/', FILE/NAME/text() )"/>
    <li>
      <a href="{$relurl}"><xsl:value-of select="FILE/NAME/text()"/></a>
    </li>
  </xsl:template>

  <xsl:template match="GRADE_HISTORY_ENTRY">
    <xsl:variable name="attemptid" select="ATTEMPT_ID/@value"/>
    <xsl:variable name="epoch" select="DATE_LOGGED/@bbcarv:epoch"/>
    <div>
      <h2><xsl:value-of select="DATE_LOGGED/@value"/></h2>
      <table>
        <tr>
          <th>Action:</th>
          <td><xsl:value-of select="ENTRY_KEY/@value"/></td>
        </tr>
      </table>
      <xsl:if test="count( FEEDBACK_TO_USER/TEXT ) = 1">
        <div class="attempt-comments">
          <xsl:apply-templates select="FEEDBACK_TO_USER"/>
        </div>      
      </xsl:if>
      <!-- In grade history private notes are marked up similar to comments in attempt  -->
      <xsl:if test="count( INSTRUCTOR_COMMENTS/TEXT ) = 1">
        <div class="attempt-notes">
          <xsl:apply-templates select="INSTRUCTOR_COMMENTS"/>
        </div>      
      </xsl:if>
    </div>
  </xsl:template>

      
  <xsl:template match="STUDENTSUBMISSION">
    <xsl:variable name="attemptid" select="../@id"/>
    <xsl:variable name="embeddedresources" select="/bbcarv:archive/manifest/resources/resource[@type = 'course/x-bb-attemptembeddedfiles' and count(//ATTEMPTID[@value = $attemptid]) &gt; 0]"/>
    <xsl:variable name="embeddedbase">
      <xsl:choose>
        <xsl:when test="count($embeddedresources) = 1">
          <xsl:value-of select="$embeddedresources[1]/@xml:base"/>
          <xsl:text>/</xsl:text>
          <xsl:value-of select="$attemptid"/>
          <xsl:text>/embedded/</xsl:text>
        </xsl:when>
        <xsl:otherwise>unknownpath/</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
 
    <xsl:call-template name="textoutput">
      <xsl:with-param name="t" select="."/>
      <xsl:with-param name="base" select="$embeddedbase"/>
      <xsl:with-param name="v" select="'@X@EmbeddedFile.location@X@'"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="wiki" mode="textonly">
    <xsl:variable name="wikibase" select="concat( ancestor::resource/@xml:base, '/wikis/group/', uniqueID/text(), '/' )"/>
    <xsl:text>Wiki content here.</xsl:text>
    <xsl:for-each select="pages/page">
      <xsl:text>Page <xsl:value-of select="position()"/></xsl:text>
      <xsl:variable name="pagebase" select="concat( $wikibase, guid/text(), '/' )"/>
      <xsl:for-each select="versions/version">
        <xsl:text>Version <xsl:value-of select="position()"/> <xsl:value-of select="name/text()"/></xsl:text>
        <xsl:text>Edited by <xsl:value-of select="createdBy"/> on <xsl:value-of select="createdOn"/></xsl:text>
        <xsl:call-template name="textoutputwiki">
          <xsl:with-param name="t" select="contentText/text()"/>
          <xsl:with-param name="base" select="$pagebase"/>
          <xsl:with-param name="v" select="'@X@EmbeddedFile.cslocation@X@'"/>
        </xsl:call-template>
      </xsl:for-each>      
    </xsl:for-each>
  </xsl:template>
    
  <xsl:template match="wiki">
    <xsl:variable name="wikibase" select="concat( ancestor::resource/@xml:base, '/wikis/group/', uniqueID/text(), '/' )"/>
    <p>Wiki content here.</p>
    <xsl:for-each select="pages/page">
      <h3>Page <xsl:value-of select="position()"/></h3>
      <xsl:variable name="pagebase" select="concat( $wikibase, guid/text(), '/' )"/>
      <xsl:for-each select="versions/version">
        <h3>Version <xsl:value-of select="position()"/> <xsl:value-of select="name/text()"/></h3>
        <p>Edited by <xsl:value-of select="createdBy"/> on <xsl:value-of select="createdOn"/></p>
        <xsl:call-template name="textoutputwiki">
          <xsl:with-param name="t" select="contentText/text()"/>
          <xsl:with-param name="base" select="$pagebase"/>
          <xsl:with-param name="v" select="'@X@EmbeddedFile.cslocation@X@'"/>
        </xsl:call-template>
      </xsl:for-each>      
    </xsl:for-each>
  </xsl:template>
    
  <xsl:template match="INSTRUCTORCOMMENTS">
    <xsl:variable name="attemptid" select="../@id"/>
    <xsl:variable name="embeddedbase" select="concat( 'csfiles/privatedoc/gradebook/notesAndFeedback/attempt/', $attemptid, '/' )"/>
    <xsl:call-template name="textoutput">
      <xsl:with-param name="t" select="."/>
      <xsl:with-param name="base" select="$embeddedbase"/>
      <xsl:with-param name="v" select="'@X@EmbeddedFile.cslocation@X@'"/>
    </xsl:call-template>
  </xsl:template>
    
  <xsl:template match="FEEDBACK_TO_USER">
    <xsl:variable name="attemptid" select="../ATTEMPT_ID/@value"/>
    <xsl:variable name="attempt" select="$gradebook/OUTCOMEDEFINITIONS/OUTCOMEDEFINITION/OUTCOMES/OUTCOME/ATTEMPTS/ATTEMPT[@id = $attemptid]"/>
    <xsl:variable name="outcomeid" select="$attempt/../../@id"/>
    <xsl:variable name="epoch" select="../DATE_LOGGED/@bbcarv:epoch"/>
    <xsl:variable name="embeddedbase" select="concat( 'csfiles/privatedoc/gradebook/notesAndFeedback/gradeHistory/', $outcomeid, '/', $epoch, '/' )"/>
    <xsl:call-template name="textoutput">
      <xsl:with-param name="t" select="."/>
      <xsl:with-param name="base" select="$embeddedbase"/>
      <xsl:with-param name="v" select="'@X@EmbeddedFile.cslocation@X@'"/>
    </xsl:call-template>
  </xsl:template>
    
  <xsl:template match="FEEDBACK_TO_USER" mode="override">
    <xsl:variable name="outcomeid" select="../@id"/>
    <xsl:variable name="embeddedbase" select="concat( 'csfiles/privatedoc/gradebook/notesAndFeedback/gradeDetail/', $outcomeid, '/' )"/>
    <xsl:call-template name="textoutput">
      <xsl:with-param name="t" select="."/>
      <xsl:with-param name="base" select="$embeddedbase"/>
      <xsl:with-param name="v" select="'@X@EmbeddedFile.cslocation@X@'"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template match="INSTRUCTORNOTES">
    <xsl:variable name="attemptid" select="../@id"/>
    <xsl:variable name="embeddedbase" select="concat( 'csfiles/privatedoc/gradebook/notesAndFeedback/attemptNotes/', $attemptid, '/' )"/>
    <xsl:call-template name="textoutput">
      <xsl:with-param name="t" select="."/>
      <xsl:with-param name="base" select="$embeddedbase"/>
      <xsl:with-param name="v" select="'@X@EmbeddedFile.cslocation@X@'"/>
    </xsl:call-template>
  </xsl:template>

  <!-- Grade history - BUT USED FOR PRIVATE NOTES!!!!!  -->
  <xsl:template match="INSTRUCTOR_COMMENTS">
    <xsl:variable name="attemptid" select="../ATTEMPT_ID/@value"/>
    <xsl:variable name="attempt" select="$gradebook/OUTCOMEDEFINITIONS/OUTCOMEDEFINITION/OUTCOMES/OUTCOME/ATTEMPTS/ATTEMPT[@id = $attemptid]"/>
    <xsl:variable name="outcomeid" select="$attempt/../../@id"/>
    <xsl:variable name="epoch" select="../DATE_LOGGED/@bbcarv:epoch"/>
    <xsl:variable name="embeddedbase" select="concat( 'csfiles/privatedoc/gradebook/notesAndFeedback/gradeHistoryNotes/', $outcomeid, '/', $epoch, '/' )"/>
    <xsl:call-template name="textoutput">
      <xsl:with-param name="t" select="."/>
      <xsl:with-param name="base" select="$embeddedbase"/>
      <xsl:with-param name="v" select="'@X@EmbeddedFile.cslocation@X@'"/>
    </xsl:call-template>
  </xsl:template>
    
  <!-- Grade history - BUT USED FOR PRIVATE NOTES!!!!!  -->
  <xsl:template match="INSTRUCTOR_COMMENTS" mode="override">
    <xsl:variable name="outcomeid" select="../@id"/>
    <xsl:variable name="embeddedbase" select="concat( 'csfiles/privatedoc/gradebook/notesAndFeedback/gradeDetailNotes/', $outcomeid, '/' )"/>
    <xsl:call-template name="textoutput">
      <xsl:with-param name="t" select="."/>
      <xsl:with-param name="base" select="$embeddedbase"/>
      <xsl:with-param name="v" select="'@X@EmbeddedFile.cslocation@X@'"/>
    </xsl:call-template>
  </xsl:template>


  <xsl:template match="RUBRIC_EVALUATION">
    <xsl:variable name="rubeval" select="."/>
    <xsl:variable name="embeddedbase" select="./ancestor::resource/@xml:base"/>    
    <xsl:variable name="rubricid" select="RUBRIC_ID/@value"/>
    <xsl:variable name="rubric"   select="/bbcarv:archive/manifest/resources/resource/bbcarv:data/LEARNRUBRICS/Rubric[@id = $rubricid]"/>
	
	<h3>RUBRIC</h3>
    <table>
      <xsl:for-each select="$rubric/RubricRows/Row">
        <xsl:sort select="Position/@value"/>
        <xsl:variable name="rowid" select="@id"/>
        <xsl:variable name="maxscore" select="number( Percentage/@value ) * 1.0"/>
        <tr><th colspan="2" style="text-align: left; padding-top: 1em;"><xsl:value-of select="./Header/@value"/></th></tr>
        <xsl:for-each select="RubricColumns/Column">
          <xsl:sort select="Position/@value"/>
          <xsl:variable name="cellid" select="Cell/@id"/>
          <xsl:variable name="evalcell" select="$rubeval/RUBRIC_EVAL/RUBRIC_CELL_EVAL[RUBRIC_CELL_ID/@value = $cellid]"/>
          <xsl:if test="count($evalcell) = 1">
            <tr><th>Category</th><td><xsl:value-of select="./Header/@value"/></td></tr>
			<tr><th>Standard Feedback</th><td><xsl:value-of select="./Cell/CellDescription/@value"/></td></tr>
			<tr>
			  <th>Score</th>
			  <td>
			    <xsl:value-of select="number( $evalcell/SELECTED_PERCENT/@value ) * 100.0"/>% ( max available <xsl:value-of select="$maxscore"/>% )
			  </td>
			</tr>
            <tr>
		      <th>Personalised Feedback</th>
			  <td>
                <xsl:call-template name="textoutputgeneral">
                  <xsl:with-param name="original" select="$evalcell/FEEDBACK/FEEDBACK_TEXT/text()"/>
                  <xsl:with-param name="type" select="$evalcell/FEEDBACK/FLAGS/FORMATTED_TYPE/@value"/>
                  <xsl:with-param name="base" select="$embeddedbase"/>
                  <xsl:with-param name="v" select="'@X@EmbeddedFile.cslocation@X@'"/>
                </xsl:call-template>
			  </td>
			</tr>
		  </xsl:if>
        </xsl:for-each>
      </xsl:for-each>
    </table>
  </xsl:template>

  <xsl:template name="textoutput">
    <xsl:param name="t"/>
    <xsl:param name="base"/>
    <xsl:param name="v"/>
    <xsl:variable name="original" select="TEXT/text()"/>
    <xsl:call-template name="textoutputgeneral">
      <xsl:with-param name="original" select="$original"/>
      <xsl:with-param name="type" select="$t/TYPE/@value"/>
      <xsl:with-param name="base" select="$base"/>
      <xsl:with-param name="v" select="'@X@EmbeddedFile.cslocation@X@'"/>
    </xsl:call-template>
  </xsl:template>

  <xsl:template name="textoutputgeneral">
    <xsl:param name="original"/>
    <xsl:param name="type"/>
    <xsl:param name="base"/>
    <xsl:param name="v"/>
    <xsl:variable name="rebased" select="java:uk.ac.leedsbeckett.bubblecarver.xsl.StringProcessor.replace( $original, $v, $base )"/>
    
    <xsl:choose>
      <xsl:when test="$type = 'H'">
        <div class="userhtml">
          <xsl:value-of select="$rebased" disable-output-escaping="yes"/>
        </div>
      </xsl:when>
      <xsl:otherwise>
        <div class="userplaintext">
          <xsl:value-of select="$original"/>        
        </div>
      </xsl:otherwise>
    </xsl:choose>
  </xsl:template>


  <xsl:template name="textoutputwiki">
    <xsl:param name="t"/>
    <xsl:param name="base"/>
    <xsl:param name="v"/>
    <xsl:variable name="rebased" select="java:uk.ac.leedsbeckett.bubblecarver.xsl.StringProcessor.replace( $t, $v, $base )"/>
    
    <div class="userhtml">
      <xsl:value-of select="$rebased" disable-output-escaping="yes"/>
    </div>
  </xsl:template>
    
    
  <xsl:template match="*">
    <xsl:text>UNEXPECTED XSLT PROCESSING</xsl:text>
  </xsl:template>  
  
</xsl:stylesheet>
