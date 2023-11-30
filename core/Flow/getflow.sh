if [ -z ${1+x} ];
then
	echo "Usage: getflow [ip of instance] [private key path] (optional)"
else
	if [ -z ${2+x} ];
	then 
		scp azure@${1}:/opt/nifi-latest/conf/flow.xml.gz ./newflow.xml.gz
	else
		scp -i "${2}" azure@${1}:/home/azure/flows/flow.xml.gz ./newflow.xml.gz
	fi
fi