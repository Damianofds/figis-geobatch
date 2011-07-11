CREATE TABLE "FIGIS_GIS"."STATISTICAL_TABLE" 
   (    "ID" NUMBER PRIMARY KEY, 
    "SRC_NAME" VARCHAR2(100 BYTE), 
    "SRC_CODE" VARCHAR2(255 BYTE), 
    "TOT_AREA_SRC" NUMBER, 
    "TRG_NAME" VARCHAR2(100 BYTE), 
    "TRG_CODE" VARCHAR2(255 BYTE), 
    "TOT_AREA_TRG" NUMBER, 
    "AREA" NUMBER, 
    "OV_SRC" NUMBER, 
    "OV_TRG" NUMBER
   );
 
 
 
CREATE TABLE "FIGIS_GIS"."SPATIAL_TABLE" 
   (    "ID" NUMBER PRIMARY KEY, 
    "THE_GEOM" "MDSYS"."SDO_GEOMETRY" , 
    "STATS" NUMBER
   );
 


CREATE OR REPLACE FORCE VIEW "FIGIS_GIS"."SPATIAL_STATISTICAL" ("ID",
"SRC_NAME", "SRC_CODE", "TOT_AREA_SRC", "TRG_NAME", "TRG_CODE", "TOT_AREA_TRG",
"AREA", "OV_SRC", "OV_TRG", "THE_GEOM") AS 
  select ST.ID, ST.SRC_NAME, ST.SRC_CODE, ST.TOT_AREA_SRC, ST.TRG_NAME,
     ST.TRG_CODE, ST.TOT_AREA_TRG, ST.AREA, ST.OV_SRC, ST.OV_TRG, SP.THE_GEOM
   from STATISTICAL_TABLE ST, SPATIAL_TABLE SP
   where ST.ID = SP.STATS with check option;
