
echo "Using file "$1"!"

java -cp target/aistr.jar nl.tudelft.instrumentation.Main --type=fuzzing --file=$1.java > instrumented/$1.java

javac -cp target/aistr.jar:. Errors.java instrumented/$1.java

java -cp target/aistr.jar:./instrumented:. $1

