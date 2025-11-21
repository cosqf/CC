#run CORE and load the topology
#start the session
#run setup.sh while on CC using ./scripts/setup.sh (on a terminal outside CORE)

#compiles the source code
javac -d out/ src/main/java/**/*.java

#copies the mothership script to the mothership's configuration folder
cp scripts/mothership.sh /tmp/pycore.*/NaveMae.conf

#copies the rover script to every configuration folder except the mothership's, the ground control's, and the satellites'
#which is to say, copies it to the rovers' configuration folders
for d in $(find /tmp/pycore.* -maxdepth 1 -type d -name "*.conf" \
              -not -name "NaveMae.conf" \
              -not -name "GC.conf" \
              -not -name "Satelite*.conf"); do
    cp scripts/rover.sh "$d/"
done