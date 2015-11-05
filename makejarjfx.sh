#!/bin/bash
SCRIPTTITLE="JavaFX RISK build-building script v1.3";
JDKERR="Did you add the JDK location to your \$PATH variables?\nsomething like...\nexport PATH=\$PATH:\"/cygdrive/C/Program Files/Java/jdk1.8.0_45/bin/\""

#Depending on platform, this may be used to compile + run the JavaFX version of Risk.
#If it doesn't work for your platform, it still gives you guidance on what needs to happen
#in order to compile/archive (create a Jar) + run the JavaFX version of Risk from the command line.
printf "\n"
printf "$SCRIPTTITLE"
printf "\n"

#rudimentary check for correct folder.
echo "   Validating script location..."
if ! test /src/ ; then
	echo "***Not in root folder/could not find source folder! Script cannot work."
else
echo "   Script may be in correct folder! Proceeding..."

#
#clear the previous build folders, if it exists
echo "   Cleaning previous build files, if exist."
rm -rf build

#find all the source files in the project
#and store them as a list in a text file
echo "   Locating source files..."
find . -name "*.java" > srcFiles.txt
echo "   ..."

#prepare a destination folder for Java CLASS files
mkdir build

#prepare the Java CLASS files from the source files, using the
#list of files found in srcFiles.txt
#flags: "-g": required to generate debugging info due to obscure bug
# introduced by using "final String" variables in source code at select spots.
echo "   Preparing source files for compilation. . ."
javac -g -d build @srcFiles.txt
if [ ! $? -eq 0 ]
then
	printf "$JDKERR"
	echo "   ...?"
else
	echo "   ...OK!"
fi

#create a manifest, in preparation for the creation of the final Jar.
#This points to the class containing the main() method we desire.
#In this case, we want the full JavaFX game, so point to FXUIGameMaster.
#(PackageName.ClassNameWithMainFunction =yields=> Master.FXUIGameMaster)
echo Main-Class: Master.FXUIGameMaster >manifest.txt

#copy extra resource files to the BUILD folder, to be added
#to the Jar file. Output will be noisy.
echo "   Copying resources to build folder. (benign errors may occur)."
cp **/RiskBoard*.jpg RiskBoard*.jpg **/TextNodes.txt TextNodes.txt manifest.txt build
cp **/Icon.jpg Icon.jpg build
cp *.m4a **/*.m4a **/**/*.m4a **/**/**/*.m4a build

#move into the build folder for proper compilation
cd build

#package everything into an easy-to-use Jar file.
#includes additional resources such as the map & list of countries.
echo "   Attempting to create runnable Jar . . ."
jar cvfm ../App.jar manifest.txt **/*.class *.txt *.jpg *.m4a
if [ ! $? -eq 0 ]
then
	printf "Minor error occurred. Might have been major. We’ll see."
	echo "   ..."
else
	echo -e "   ...OK!"
fi

#return to the main project folder, where the jar should exist
cd ..

#run the application from the newly-created Jar file.
printf "   If successful, launching game!\n\n\n"
java -jar App.jar

#ask if the user wants to clean the extraneous build files
printf "\n\n\n   Would you like to remove the build files?\n"
printf "   (Executable will be left in place) :: y/n: "
read cleanBFiles

approvalStr="y";
if [[ $cleanBFiles =~ $approvalStr ]]
then
    printf "   removing extraneous build-related files. . .\n"
    printf "   application jar left in place. \n"
    rm -rf build
    rm -r manifest.txt
    rm -r srcFiles.txt
else
    printf "   build files left in place.\n"
fi

printf "   Thank you & Good-bye!\n"
fi