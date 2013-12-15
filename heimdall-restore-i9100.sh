if [ -f i9100.pit ] & [ -f zImage ] & [ -f factoryfs.img ] & [ -f data.img ] & [ -f hidden.img ]
then 
	echo "checking files ..."
	if [ "`md5sum zImage | cut -d ' ' -f 1`" = "`cat zImage.md5 | cut -d ' ' -f 1`" ] &
		[ "`md5sum factoryfs.img | cut -d ' ' -f 1`" = "`cat factoryfs.img.md5 | cut -d ' ' -f 1`" ] &
		[ "`md5sum data.img | cut -d ' ' -f 1`" = "`cat data.img.md5 | cut -d ' ' -f 1`" ] &
		[ "`md5sum hidden.img | cut -d ' ' -f 1`" = "`cat hidden.img.md5 | cut -d ' ' -f 1`" ]
	then
	heimdall flash -pit i9100.pit --KERNEL zImage --FACTORYFS factoryfs.img --DATAFS data.img --HIDDEN hidden.img --verbose
	else
	echo "md5 mismatch ..."
	fi
else
	echo "missing files ..."
fi
