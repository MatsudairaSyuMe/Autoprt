package com.systex.sysgateii.gateway.util;

public class dataUtil {
	public int ArraySearchIndexOf(final byte[] outerArray, final byte[] smallerArray) {
		for (int i = 0; i < outerArray.length - smallerArray.length + 1; ++i) {
			boolean found = true;
			for (int j = 0; j < smallerArray.length; ++j) {
				if (outerArray[i + j] != smallerArray[j]) {
					found = false;
					break;
				}
			}
			if (found)
				return i;
		}
		return -1;
	}

	public static int fromByteArray(byte[] bytes) {
		int r = 0;
		for (byte b : bytes)
			r = (r * 100) + ((((b >> 4) & 0xf) * 10 + (b & 0xf)));
		return r;
	}

	public static byte[] to3ByteArray(int l) {
		byte[] rtn = new byte[3];
		int tl = l;
		byte b1 = (byte) 0x0;
		byte b2 = (byte) 0x0;
		for (int i = rtn.length - 1; i >= 0; --i) {
			b1 = (byte) (tl % 10);
			tl = tl / 10;
			b2 = (byte) (tl % 10);
			tl = tl / 10;
			rtn[i] = (byte) (((b2 << 4) & 0xf0) | (b1 & 0xf));
		}
		return rtn;
	}
	/*********************************************************
	*  rfmtdbl()   : 金額格式化                              *
	*  parameter 1 : input data                              *
	*  parameter 2 : input data format                       *
	*  parameter 3 : ouput data buff                         *
	*  return_code :                  (參考TPCOMM) 2008.01.25*
	*********************************************************/
/*	public static byte[] rfmtdbl(double idbl, byte[] ifmt)
	{
		int	i;
		int	Lcommacnt,Ldec,Ldot,obuflen,Lheading,Lminus,LIfmt;
		int	Ldollarsign,Lstarsign;
		byte[]	Ltfmt = new byte[30],Lfmt = new byte[30],Lifmt = new byte[40];
		byte[]	Ltbuf = new byte[50];
		double   Lidbl;
		byte[]	p1,p2,p3,p4,p5,p6,p7;

		Ldollarsign = 0;
		Lstarsign = 0;
		Lminus = 0;
		Lidbl = idbl;

		if ( Lidbl < 0 )
			Lidbl *= -1;

		strcpy(Lifmt,ifmt);
		LIfmt = strlen(Lifmt);
		for(i=0; i < LIfmt; i++)
		{
			if ( Lifmt[i] == '$' )
			{
				Ldollarsign=1;
				Lifmt[i] = 'Z';
			}
			else if ( Lifmt[i] == '*' )
			{
				Lstarsign=1;
				Lifmt[i] = 'Z';
			}
			else if ( Lifmt[i] == '-' )
			{
				Lminus=1;
				Lifmt[i] = 'Z';
			}
		}

		strcpy(Lfmt,Lifmt);
		obuflen=strlen(Lifmt);

		p1=strtok(Lfmt,".");
		if ( p1 == NULL )
		{
			memcpy(obuf,STAR,obuflen);
			return(-1);
		}
		p2=strtok(NULL,".");

		Ldec=strlen(p1);
		if ( p2 != NULL )
			Ldot=strlen(p2);
		else
			Ldot=0;

		Lcommacnt=0;
		for( i = 0 ; i < Ldec ; i++ )
		{
			if ( *(p1+i) == ',' )
				Lcommacnt++;
		}

		if ( Ldot == 0 )
			sprintf(Ltfmt,"%%%d.%df", Ldec - Lcommacnt , Ldot );
		else
			sprintf(Ltfmt,"%%%d.%df", Ldec - Lcommacnt + Ldot +1 , Ldot );
		sprintf(Ltbuf,Ltfmt,Lidbl);

		p3=strtok(Ltbuf,".");
		p4=strtok(NULL,".");

		LIfmt = strlen(p3);
		if ( LIfmt > Ldec )
		{
			memcpy(obuf,STAR,obuflen);
			return(-1);
		}

		p5= p3 + strlen(p3) -1;

		Lheading=0;
		p6=p1+Ldec-1;
		p7=obuf+Ldec;
		for(i=0; i< Ldec; i++)
		{

			switch( *p6 )
			{
				case '9' :
						*(--p7) = *p5;
						if ( *p7 == '-' )
						  *p7 = '0';
						else
						if ( *p7 == ' ' )
						  *p7 = '0';
						p5--;
						break;
				case '-' :
				case 'Z' :
						*(--p7) = *p5;
						if ( *p7 == '+' )
						  *p7 = ' ';
						p5--;
						break;
				case ',' :
						if ( *(p6-1) == '9')
						  *(--p7) = ',';
						else
						if ( *p5 == ' ')
						  *(--p7) = ' ';
						else
						  *(--p7) = ',';
						break;
				default :
						*(--p7) = *p5;
						p5--;
						break;
			}
			p6--;
		}

		if ( Lminus )
		{
			if ( *idbl < 0 )
			{
				for(i=1;i<Ldec;i++)
				{
					if ( *(obuf+i) != ' ')
					{
						*(obuf+i-1) = (*(obuf+i-1) != ' ') ? *(obuf+i-1) : '-';
						break;
					}
				}
			}
		}
		if ( Ldollarsign )
		 {
			for(i=1;i<Ldec;i++)
			{
				if ( *(obuf+i) != ' ')
				{
					*(obuf+i-1) = '$';
					break;
				}
			}
		}
		else if ( Lstarsign )
		{
			for(i=1;i<Ldec;i++)
			{
				if ( *(obuf+i) != ' ')
				{
					*(obuf+i-1) = '*';
					break;
				}
			}
		}

		if ( p2 != NULL )
		{
			obuf[Ldec]='.';
			memcpy(&obuf[Ldec+1],p4,Ldot);
		}
		return obuf;
	}*/
}
