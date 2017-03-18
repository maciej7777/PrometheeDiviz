# Adapt the two following lines -- NB: Java 8 is required
JAVA_HOME="/usr/jdk/jdk1.8.0_112"
XMCDA_LIB="libs/XMCDA-java-0.2.0-73-g5ceb908.jar"
CLASSPATH="out/production/PROMETHEE-FlowSort-GDSS_assignments"

# -- You normally do not need to change anything beyond this point --

JAVA=${JAVA_HOME}/bin/java

# check that the XMCDA library can be found and that Java is executable
if [ ! -f ${XMCDA_LIB} -o "" == "${JAVA_HOME}" ]; then
  echo "Please modify common_settings.sh to reflect your installation (see README)" >&2
  exit -1;
fi
if [ ! -x "${JAVA}" ]; then
  echo "Java: ${JAVA}: not found -- please edit common_settings.sh and check JAVA_HOME"
  exit -1;
fi

CLASSPATH="./bin:${XMCDA_LIB}:${CLASSPATH}"
CMD="${JAVA} -cp ${CLASSPATH} pl.poznan.put.promethee.xmcda.FlowSortGDSSCLI"
export JAVA_HOME