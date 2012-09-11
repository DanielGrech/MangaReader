#!/usr/bin/python

"""
TODO: Before using this script, should 
remove all '{http://toiletmap.gov.au/}' from the source file
"""

import sys
import sqlite3
import urllib2
import json


########## ----------- MAIN ----------- ##########

conn = sqlite3.connect('manga.db')
c = conn.cursor()
c.execute('''
			CREATE TABLE series
			(
				_id integer primary key,
				series_id text,
				image text,
				title text collate nocase,
				description text,
				author text,
				last_chapter_date integer,
				created_date integer
			);

             ''')

c.execute('''
          CREATE TABLE favourite_series
           (
               _id integer primary key,
               series_id integer,
               is_favourite integer default 0
           );
          ''')

c.execute('''
          CREATE TABLE chapters
           (
               _id integer primary key,
               series_id text,
               chapter_id text,
               title text collate nocase,
               chapter_sequence_num integer,
               release_date integer
           );
          ''')

c.execute('''
          CREATE TABLE pages
           (
               _id integer primary key,
               chapter_id text,
               page_number integer,
               image text
           );
          ''')

c.execute('''
          CREATE TABLE categories
           (
               _id integer primary key,
               category text,
               series_id integer
           );
          ''')

c.execute(''' CREATE UNIQUE INDEX "unique_series" ON series(series_id); ''')
c.execute(''' CREATE UNIQUE INDEX "unique_series_favourite" ON favourite_series(series_id); ''')
c.execute(''' CREATE UNIQUE INDEX "unique_chapter" ON chapters(series_id, chapter_id); ''')
c.execute(''' CREATE UNIQUE INDEX "unique_page" ON pages(chapter_id, page_number); ''')


IMAGE_BASE_URL = "http://cdn.mangaeden.com/mangasimg/"
SERIES_URL = "http://www.mangaeden.com/api/list/0/"
data = urllib2.urlopen(SERIES_URL).read()
manga = json.loads(data)["manga"]

rows_to_insert = []
for series in manga:
    rows_to_insert.append((
        "" if series["i"] is None else series["i"],
        "" if series["im"] is None else (IMAGE_BASE_URL + series["im"]),
        "" if series["t"] is None else series["t"]
    ));

c.executemany('''
				INSERT INTO series(series_id, image, title)
				VALUES (?, ?, ?)''', rows_to_insert)

conn.commit()
c.close()


