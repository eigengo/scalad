<?xml version="1.0" encoding="UTF-8"?>

<!--
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
				xmlns:fo="http://www.w3.org/1999/XSL/Format"
				xmlns:xslthl="http://xslthl.sf.net"
				exclude-result-prefixes="xslthl"
				version='1.0'>

	<xsl:import href="urn:docbkx:stylesheet" />
	<xsl:import href="urn:docbkx:stylesheet/highlight.xsl"/>

	<xsl:param name="highlight.source" select="1"/>
	<!-- Use nice graphics for admonitions -->
	<xsl:param name="admon.graphics">'1'</xsl:param>
	<xsl:param name="admon.graphics.path">@file.prefix@@dbf.xsl@/images/</xsl:param>
	<xsl:param name="draft.watermark.image" select="'@file.prefix@@dbf.xsl@/images/draft.png'"/>
	<xsl:param name="paper.type" select="'@paper.type@'"/>

	<xsl:param name="page.margin.top" select="'1cm'"/>
	<xsl:param name="region.before.extent" select="'1cm'"/>
	<xsl:param name="body.margin.top" select="'1.5cm'"/>

	<xsl:param name="body.margin.bottom" select="'1.5cm'"/>
	<xsl:param name="region.after.extent" select="'1cm'"/>
	<xsl:param name="page.margin.bottom" select="'1cm'"/>
	<xsl:param name="title.margin.left" select="'0cm'"/>

<!--###################################################
		Table of Contents
	################################################### -->

	<xsl:param name="generate.toc">
		book      toc,title
	</xsl:param>

<!--###################################################
		Custom Header
	################################################### -->

	<xsl:template name="header.content">
		<xsl:param name="pageclass" select="''"/>
		<xsl:param name="sequence" select="''"/>
		<xsl:param name="position" select="''"/>
		<xsl:param name="gentext-key" select="''"/>

		<xsl:variable name="Version">
			<xsl:choose>
				<xsl:when test="//productname">
					<xsl:value-of select="//productname"/><xsl:text> </xsl:text>
				</xsl:when>
				<xsl:otherwise>
					<xsl:text>Specs2 Spring</xsl:text>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		<xsl:choose>
			<xsl:when test="$sequence='blank'">
				<xsl:choose>
					<xsl:when test="$position='center'">
						<xsl:value-of select="$Version"/>
					</xsl:when>

					<xsl:otherwise>
						<!-- nop -->
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>

			<xsl:when test="$pageclass='titlepage'">
				<!-- nop: other titlepage sequences have no header -->
			</xsl:when>

			<xsl:when test="$position='center'">
				<xsl:value-of select="$Version"/>
			</xsl:when>

			<xsl:otherwise>
				<!-- nop -->
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

<!--###################################################
		Custom Footer
	################################################### -->

	<xsl:template name="footer.content">
		<xsl:param name="pageclass" select="''"/>
		<xsl:param name="sequence" select="''"/>
		<xsl:param name="position" select="''"/>
		<xsl:param name="gentext-key" select="''"/>

		<xsl:variable name="Version">
			<xsl:choose>
				<xsl:when test="//releaseinfo">
					<xsl:value-of select="//releaseinfo"/>
				</xsl:when>
				<xsl:otherwise>
					<!-- nop -->
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		<xsl:variable name="Title">
			<xsl:value-of select="//title"/>
		</xsl:variable>

		<xsl:choose>
			<xsl:when test="$sequence='blank'">
				<xsl:choose>
					<xsl:when test="$double.sided != 0 and $position = 'left'">
						<xsl:value-of select="$Version"/>
					</xsl:when>

					<xsl:when test="$double.sided = 0 and $position = 'center'">
						<!-- nop -->
					</xsl:when>

					<xsl:otherwise>
						<fo:page-number/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:when>

			<xsl:when test="$pageclass='titlepage'">
				<!-- nop: other titlepage sequences have no footer -->
			</xsl:when>

			<xsl:when test="$double.sided != 0 and $sequence = 'even' and $position='left'">
				<fo:page-number/>
			</xsl:when>

			<xsl:when test="$double.sided != 0 and $sequence = 'odd' and $position='right'">
				<fo:page-number/>
			</xsl:when>

			<xsl:when test="$double.sided = 0 and $position='right'">
				<fo:page-number/>
			</xsl:when>

			<xsl:when test="$double.sided != 0 and $sequence = 'odd' and $position='left'">
				<xsl:value-of select="$Version"/>
			</xsl:when>

			<xsl:when test="$double.sided != 0 and $sequence = 'even' and $position='right'">
				<xsl:value-of select="$Version"/>
			</xsl:when>

			<xsl:when test="$double.sided = 0 and $position='left'">
				<xsl:value-of select="$Version"/>
			</xsl:when>

			<xsl:when test="$position='center'">
				<xsl:value-of select="$Title"/>
			</xsl:when>

			<xsl:otherwise>
				<!-- nop -->
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template match="processing-instruction('hard-pagebreak')">
		<fo:block break-before='page'/>
	</xsl:template>


</xsl:stylesheet>