<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                version="1.0">

    <xsl:import href="urn:docbkx:stylesheet"/>

    <!-- auto section numbering -->  
    <xsl:param name="section.autolabel" />
    
    <!-- fix hyper-links -->
    <xsl:param name="ulink.show" select="0" />
    <xsl:attribute-set name="xref.properties">
        <xsl:attribute name="color">blue</xsl:attribute>
    </xsl:attribute-set>
    
</xsl:stylesheet>
