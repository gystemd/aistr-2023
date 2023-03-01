
echo "Using file ""$1""!"

cp problems/"$1".java "$1".java

java -cp target/aistr.jar nl.tudelft.instrumentation.Main --type=symbolic --file="$1".java > instrumented/"$1".java

echo "Compiling"

javac -cp target/aistr.jar:lib/com.microsoft.z3.jar:. Errors.java instrumented/"$1".java

# javac -cp target/aistr.jar:. Errors.java instrumented/"$1".java

echo "Running"

java -cp target/aistr.jar:lib/com.microsoft.z3.jar:./instrumented "$1"

rm $1.java
