echo "Removing old database.."
rm manga.db 

echo "Downloading data.."
./SeriesToSQLite.py 

echo "Zipping.."
zip manga.zip manga.db 

echo "Moving result to assets.."
mv manga.zip ../assets/databases

echo "DONE!"