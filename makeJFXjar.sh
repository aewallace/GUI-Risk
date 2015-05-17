#!/bin/bash

#Depending on platform, this may be used to compile + run the JavaFX version of Risk.
#If it doesn't work for your platform, it still gives you guidance on what needs to happen
#in order to compile/archive (create a Jar) + run the JavaFX version of Risk from the command line.

#rudimentary check for correct folder.
echo "Checking location..."
#if ! cd "src" ; then
if ! test /src/ ; then
	echo "Not in root folder/could not find source folder! Script cannot work."
else
echo "Script may be in correct folder! Proceeding..."
#cd .. #undo the "test"

#
#clear the previous build folders, if it exists
echo "Cleaning previous build files, if exist."
rm -rf build

#find all the source files in the project
#and store them as a list in a text file
find . -name "*.java" > srcFiles.txt
echo "..."

#prepare a destination folder for Java CLASS files
mkdir build

#prepare the Java CLASS files from the source files, using the
#list of files found in srcFiles.txt
echo "Locating source files..."
javac -d build @srcFiles.txt

#create a manifest, in preparation for the creation of the final Jar.
#This points to the class containing the main() method we desire.
#In this case, we want the full JavaFX game, so point to FXUIGameMaster.
#(PackageName.ClassNameWithMainFunction =yields=> Master.FXUIGameMaster)
echo Main-Class: Master.FXUIGameMaster >manifest.txt

#copy extra resource files to the BUILD folder, to be added
#to the Jar file. Output will be noisy.
echo "Copying resources to build folder. (benign errors may occur)."
cp **/*.jpg *.jpg **/*.txt *.txt build

cd build

#package everything into an easy-to-use Jar file.
#includes additional resources such as the map & list of countries.
echo "Attempting to create runnable Jar..."
jar cvfm ../App.jar manifest.txt **/*.class *.txt *.jpg

cd ..

#run the application from the newly-created Jar file.
echo "If successful, launching game!"
java -jar App.jar

echo "Good-bye!"
fi