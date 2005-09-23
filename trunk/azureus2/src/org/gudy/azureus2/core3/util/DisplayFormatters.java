/*
 * File    : DisplayFormatters.java
 * Created : 07-Oct-2003
 * By      : gardnerpar
 *
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.core3.util;

/**
 * @author gardnerpar
 *
 */

import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.text.NumberFormat;

import org.gudy.azureus2.core3.download.*;
import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.disk.*;
import org.gudy.azureus2.core3.internat.*;

public class
DisplayFormatters
{
	final public static int UNIT_B  = 0;
	final public static int UNIT_KB = 1;
	final public static int UNIT_MB = 2;
	final public static int UNIT_GB = 3;
	final public static int UNIT_TB = 4;
	
	final protected static int UNITS_PRECISION[] =	 {	 0, // B
	                                                     1, //KB
	                                                     1, //MB
	                                                     2, //GB
	                                                     3 //TB
	                                                  };
	protected static String[] units;
	protected static String[] units_rate;
	protected static int unitsStopAt = UNIT_TB;

	protected static String[] units_base10;
	
	private static String		per_sec;
	
	protected static boolean use_si_units;
	protected static boolean use_units_rate_bits;
    protected static boolean not_use_GB_TB;

	// private static String lastDecimalFormat = "";

	static{
		use_si_units = COConfigurationManager.getBooleanParameter("config.style.useSIUnits", false);

		COConfigurationManager.addParameterListener( "config.style.useSIUnits",
				new ParameterListener()
				{
					public void
					parameterChanged(
						String	value )
					{
						use_si_units = COConfigurationManager.getBooleanParameter("config.style.useSIUnits", false);

						setUnits();
					}
				});

		use_units_rate_bits = COConfigurationManager.getBooleanParameter("config.style.useUnitsRateBits", false);

		COConfigurationManager.addParameterListener( "config.style.useUnitsRateBits",
				new ParameterListener()
				{
					public void
					parameterChanged(
						String	value )
					{
						use_units_rate_bits = COConfigurationManager.getBooleanParameter("config.style.useUnitsRateBits", false);

						setUnits();
					}
				});

    not_use_GB_TB = COConfigurationManager.getBooleanParameter("config.style.doNotUseGB", false);
    unitsStopAt = (not_use_GB_TB) ? UNIT_MB : UNIT_TB;

    COConfigurationManager.addParameterListener( "config.style.doNotUseGB",
        new ParameterListener()
        {
          public void
          parameterChanged(
            String  value )
          {
            not_use_GB_TB = COConfigurationManager.getBooleanParameter("config.style.doNotUseGB", false);
            unitsStopAt = (not_use_GB_TB) ? UNIT_MB : UNIT_TB;

						setUnits();
          }
        });

		setUnits();
	}

	static NumberFormat	percentage_format;
	
	static{
		percentage_format = NumberFormat.getPercentInstance();
		percentage_format.setMinimumFractionDigits(1);
		percentage_format.setMaximumFractionDigits(1);
	}
	
  protected static void
  setUnits()
  {
      // (1) http://physics.nist.gov/cuu/Units/binary.html
      // (2) http://www.isi.edu/isd/LOOM/documentation/unit-definitions.text

    units = new String[unitsStopAt + 1];
    units_rate = new String[unitsStopAt + 1];
    
    if ( use_si_units ){
      // fall through intentional
      switch (unitsStopAt) {
        case UNIT_TB:
          units[UNIT_TB] = getUnit("TiB");
          units_rate[UNIT_TB] = (use_units_rate_bits) ? getUnit("Tibit")  : getUnit("TiB");
        case UNIT_GB:
          units[UNIT_GB]= getUnit("GiB");
          units_rate[UNIT_GB] = (use_units_rate_bits) ? getUnit("Gibit")  : getUnit("GiB");
        case UNIT_MB:
          units[UNIT_MB] = getUnit("MiB");
          units_rate[UNIT_MB] = (use_units_rate_bits) ? getUnit("Mibit")  : getUnit("MiB");
        case UNIT_KB:
          // can be upper or lower case k
          units[UNIT_KB] = getUnit("KiB"); 
          // can be upper or lower case k, upper more consistent
          units_rate[UNIT_KB] = (use_units_rate_bits) ? getUnit("Kibit")  : getUnit("KiB");
        case UNIT_B:
          units[UNIT_B] = getUnit("B");
          units_rate[UNIT_B] = (use_units_rate_bits)  ?   getUnit("bit")  :   getUnit("B");
      }
    }else{
      switch (unitsStopAt) {
        case UNIT_TB:
          units[UNIT_TB] = getUnit("TB");
          units_rate[UNIT_TB] = (use_units_rate_bits) ? getUnit("Tbit")  : getUnit("TB");
        case UNIT_GB:
          units[UNIT_GB]= getUnit("GB");
          units_rate[UNIT_GB] = (use_units_rate_bits) ? getUnit("Gbit")  : getUnit("GB");
        case UNIT_MB:
          units[UNIT_MB] = getUnit("MB");
          units_rate[UNIT_MB] = (use_units_rate_bits) ? getUnit("Mbit")  : getUnit("MB");
        case UNIT_KB:
          // yes, the k should be lower case
          units[UNIT_KB] = getUnit("kB");
          units_rate[UNIT_KB] = (use_units_rate_bits) ? getUnit("kbit")  : getUnit("kB");
        case UNIT_B:
          units[UNIT_B] = getUnit("B");
          units_rate[UNIT_B] = (use_units_rate_bits)  ?  getUnit("bit")  :  getUnit("B");
      }
    }

    
    per_sec = MessageText.getString( "Formats.units.persec" );

    units_base10 = 
    	new String[]{ getUnit( "B"), getUnit("KB"), getUnit( "MB" ), getUnit( "GB"), getUnit( "TB" ) };
    
    for (int i = 0; i <= unitsStopAt; i++) {
      units[i] 		= units[i];
      units_rate[i] = units_rate[i] + per_sec;
    }
    
    NumberFormat.getPercentInstance().setMinimumFractionDigits(1);
    NumberFormat.getPercentInstance().setMaximumFractionDigits(1);
   }
  
  private static String
  getUnit(
	String	key )
  {
	  String res = " " + MessageText.getString( "Formats.units." + key );
	  	  
	  return( res );
  }

	public static String
	getRateUnit(
		int		unit_size )
	{
		return( units_rate[unit_size].substring(1, units_rate[unit_size].length()) );
	}
	public static String
	getUnit(
		int		unit_size )
	{
		return( units[unit_size].substring(1, units[unit_size].length()) );
	}

	public static String
	formatByteCountToKiBEtc(int n)
	{
		return( formatByteCountToKiBEtc((long)n));
	}

	public static
	String formatByteCountToKiBEtc(
		long n )
	{
		return( formatByteCountToKiBEtc( n, false ));
	}

	protected static
	String formatByteCountToKiBEtc(
		long	n,
		boolean	rate )
	{
		double dbl = (rate && use_units_rate_bits) ? n * 8 : n;

	  	int unitIndex = UNIT_B;
	  	
	  	while (dbl >= 1024 && unitIndex < unitsStopAt){ 
	  	
		  dbl /= 1024L;
		  unitIndex++;
		}
			 
		return( formatDecimal( dbl, UNITS_PRECISION[unitIndex] ) +  
				( rate ? units_rate[unitIndex] : units[unitIndex]));
	}

	public static String
	formatByteCountToKiBEtcPerSec(
		long		n )
	{
		return( formatByteCountToKiBEtc(n,true));
	}


		// base 10 ones

	public static String 
	formatByteCountToBase10KBEtc(
			long n) 
	{
		if (n < 1000){
			
			return n + units_base10[UNIT_B];
			
		}else if (n < 1000 * 1000){
			
			return 	(n / 1000) + "." + 
					((n % 1000) / 100) + 
					units_base10[UNIT_KB];
			
		}else if ( n < 1000L * 1000L * 1000L  || not_use_GB_TB ){
			
			return 	(n / (1000L * 1000L)) + "." +
					((n % (1000L * 1000L)) / (1000L * 100L)) +	
					units_base10[UNIT_MB];
			
		}else if (n < 1000L * 1000L * 1000L * 1000L){
			
			return (n / (1000L * 1000L * 1000L)) + "." +
					((n % (1000L * 1000L * 1000L)) / (1000L * 1000L * 100L))+
					units_base10[UNIT_GB];
			
		}else if (n < 1000L * 1000L * 1000L * 1000L* 1000L){
			
			return (n / (1000L * 1000L * 1000L* 1000L)) + "." +
					((n % (1000L * 1000L * 1000L* 1000L)) / (1000L * 1000L * 1000L* 100L))+
					units_base10[UNIT_TB];
		}else{
			
			return MessageText.getString( "Formats.units.alot" );
		}
	}

	public static String
	formatByteCountToBase10KBEtcPerSec(
			long		n )
	{
		return( formatByteCountToBase10KBEtc(n).concat(per_sec));
	}

   public static String formatETA(long eta) {
     if (eta == 0) return MessageText.getString("PeerManager.status.finished");
     if (eta == -1) return "";
     if (eta > 0) return TimeFormatter.format(eta);

     return MessageText.getString("PeerManager.status.finishedin").concat(
            " ").concat(TimeFormatter.format(eta * -1));
   }


	public static String
	formatDownloaded(
		DownloadManagerStats	stats )
	{
		long	total_discarded = stats.getDiscarded();
		long	total_received 	= stats.getTotalGoodDataBytesReceived();

		if(total_discarded == 0){

			return formatByteCountToKiBEtc(total_received);

		}else{

			return formatByteCountToKiBEtc(total_received).concat(" ( ").concat(DisplayFormatters.formatByteCountToKiBEtc(total_discarded)).concat(" ").concat(MessageText.getString("discarded")).concat(" )");
		}
	}

	public static String
	formatHashFails(
		DownloadManager		download_manager )
	{
		TOTorrent	torrent = download_manager.getTorrent();
		
		if ( torrent != null ){
			
			long bad = download_manager.getStats().getHashFailBytes();
	
					// size can exceed int so ensure longs used in multiplication
	
			long count = bad / (long)torrent.getPieceLength();
	
			String result = count + " ( " + formatByteCountToKiBEtc(bad) + " )";
	
			return result;
	  	}

  		return "";
	}

	public static String
	formatDownloadStatus(
		DownloadManager		manager )
	{
		int state = manager.getState();

		String	tmp = "";

		switch (state) {
		  case DownloadManager.STATE_WAITING :
			tmp = MessageText.getString("ManagerItem.waiting");
			break;
      case DownloadManager.STATE_INITIALIZING :
        tmp = MessageText.getString("ManagerItem.initializing");
        break;
      case DownloadManager.STATE_INITIALIZED :
        tmp = MessageText.getString("ManagerItem.initializing");
        break;
		  case DownloadManager.STATE_ALLOCATING :
			tmp = MessageText.getString("ManagerItem.allocating");
			break;
		  case DownloadManager.STATE_CHECKING :
			tmp = MessageText.getString("ManagerItem.checking");
			break;
		  case DownloadManager.STATE_FINISHING :
		    tmp = MessageText.getString("ManagerItem.finishing");
			 break;
		  case DownloadManager.STATE_READY :
			tmp = MessageText.getString("ManagerItem.ready");
			break;
		  case DownloadManager.STATE_DOWNLOADING :
			tmp = MessageText.getString("ManagerItem.downloading");
			break;
		  case DownloadManager.STATE_SEEDING :
         DiskManager diskManager = manager.getDiskManager();
         if ((diskManager != null) && diskManager.isChecking()) {
           tmp = MessageText.getString("ManagerItem.seeding").concat(
                 " + ").concat(
                 MessageText.getString("ManagerItem.checking"));
         }
         else if(manager.getPeerManager()!= null && manager.getPeerManager().isSuperSeedMode()){
           tmp = MessageText.getString("ManagerItem.superseeding");
         }
         else {
           tmp = MessageText.getString("ManagerItem.seeding");
         }
			break;
		case DownloadManager.STATE_STOPPING :
			tmp = MessageText.getString("ManagerItem.stopping");
			break;
		case DownloadManager.STATE_STOPPED :
			tmp = MessageText.getString("ManagerItem.stopped");
			break;
		  case DownloadManager.STATE_QUEUED :
			tmp = MessageText.getString("ManagerItem.queued"); //$NON-NLS-1$
			break;
		  case DownloadManager.STATE_ERROR :
			tmp = MessageText.getString("ManagerItem.error").concat(": ").concat(manager.getErrorDetails()); //$NON-NLS-1$ //$NON-NLS-2$
			break;
			default :
			tmp = String.valueOf(state);
		}

		if (manager.isForceStart() &&
		    (state == DownloadManager.STATE_SEEDING ||
		     state == DownloadManager.STATE_DOWNLOADING))
			tmp = MessageText.getString("ManagerItem.forced") + " " + tmp;
		return( tmp );
	}

	public static String
	formatDownloadStatusDefaultLocale(
		DownloadManager		manager )
	{
		int state = manager.getState();

		String	tmp = "";

		switch (state) {
		  case DownloadManager.STATE_WAITING :
			tmp = MessageText.getDefaultLocaleString("ManagerItem.waiting"); //$NON-NLS-1$
			break;
      case DownloadManager.STATE_INITIALIZING :
        tmp = MessageText.getDefaultLocaleString("ManagerItem.initializing");
        break;
      case DownloadManager.STATE_INITIALIZED :
        tmp = MessageText.getDefaultLocaleString("ManagerItem.initializing");
        break;
		  case DownloadManager.STATE_ALLOCATING :
			tmp = MessageText.getDefaultLocaleString("ManagerItem.allocating"); //$NON-NLS-1$
			break;
		  case DownloadManager.STATE_CHECKING :
			tmp = MessageText.getDefaultLocaleString("ManagerItem.checking"); //$NON-NLS-1$
			break;
		  case DownloadManager.STATE_FINISHING :
		    tmp = MessageText.getDefaultLocaleString("ManagerItem.finishing"); //$NON-NLS-1$
		    break;
         case DownloadManager.STATE_READY :
			tmp = MessageText.getDefaultLocaleString("ManagerItem.ready"); //$NON-NLS-1$
			break;
		  case DownloadManager.STATE_DOWNLOADING :
			tmp = MessageText.getDefaultLocaleString("ManagerItem.downloading"); //$NON-NLS-1$
			break;
		  case DownloadManager.STATE_SEEDING :
		  	if (manager.getDiskManager().isChecking()) {
		  		tmp = MessageText.getDefaultLocaleString("ManagerItem.seeding").concat(
		  		" + ").concat(
		  		MessageText.getDefaultLocaleString("ManagerItem.checking"));
		  	}
		  	else if(manager.getPeerManager()!= null && manager.getPeerManager().isSuperSeedMode()){

		  		tmp = MessageText.getDefaultLocaleString("ManagerItem.superseeding"); //$NON-NLS-1$
		  	}
		  	else {
		  		tmp = MessageText.getDefaultLocaleString("ManagerItem.seeding"); //$NON-NLS-1$
		  	}
		  	break;
		  case DownloadManager.STATE_STOPPING :
		  	tmp = MessageText.getDefaultLocaleString("ManagerItem.stopping");
		  	break;
		  case DownloadManager.STATE_STOPPED :
			tmp = MessageText.getDefaultLocaleString("ManagerItem.stopped"); //$NON-NLS-1$
			break;
		  case DownloadManager.STATE_QUEUED :
			tmp = MessageText.getDefaultLocaleString("ManagerItem.queued"); //$NON-NLS-1$
			break;
		  case DownloadManager.STATE_ERROR :
			tmp = MessageText.getDefaultLocaleString("ManagerItem.error").concat(": ").concat(manager.getErrorDetails()); //$NON-NLS-1$ //$NON-NLS-2$
			break;
			default :
			tmp = String.valueOf(state);
		}

		return( tmp );
	}

  public static String formatPercentFromThousands(int thousands) {
 
    return percentage_format.format(thousands / 1000.0);
  }

  public static String formatTimeStamp(long time) {
    StringBuffer sb = new StringBuffer();
    Calendar calendar = Calendar.getInstance();
    calendar.setTimeInMillis(time);
    sb.append('[');
    sb.append(formatIntToTwoDigits(calendar.get(Calendar.DAY_OF_MONTH)));
    sb.append('.');
    sb.append(formatIntToTwoDigits(calendar.get(Calendar.MONTH)+1));	// 0 based
    sb.append('.');
    sb.append(calendar.get(Calendar.YEAR));
    sb.append(' ');
    sb.append(formatIntToTwoDigits(calendar.get(Calendar.HOUR_OF_DAY)));
    sb.append(':');
    sb.append(formatIntToTwoDigits(calendar.get(Calendar.MINUTE)));
    sb.append(':');
    sb.append(formatIntToTwoDigits(calendar.get(Calendar.SECOND)));
    sb.append(']');
    return sb.toString();
  }

  public static String formatIntToTwoDigits(int n) {
    return n < 10 ? "0".concat(String.valueOf(n)) : String.valueOf(n);
  }

  public static String
  formatDate(
  	long		date )
  {
  	if ( date == 0 ){
  		return( "" );
  	}

  	SimpleDateFormat temp = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss");

  	return( temp.format(new Date(date)));
  }

  public static String
  formatDateShort(
    long    date )
  {
    if ( date == 0 ){
      return( "" );
    }

    	// 24 hour clock, no point in including AM/PM

    SimpleDateFormat temp = new SimpleDateFormat("MMM dd, HH:mm");

    return( temp.format(new Date(date)));
  }
  
  public static String
  formatDateNum(
  	long		date )
  {
  	if ( date == 0 ){
  		return( "" );
  	}

  	SimpleDateFormat temp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

  	return( temp.format(new Date(date)));
  }

  public static String
  formatTime(
    long    time )
  {
    return( TimeFormatter.formatColon( time / 1000 ));
  }

  public static String
  formatDecimal(
  	double value, 
  	int		precision )
  {
    // this call returns a cached instance.. however, it might be worth
    // checking if caching the object ourselves gives any noticable perf gains.
  	/* Don't use DecimalFormat - it ROUNDS which is not what we want
    NumberFormat nf = NumberFormat.getInstance();
    if (!lastDecimalFormat.equals(sFormat) && (nf instanceof DecimalFormat)) {
      ((DecimalFormat)nf).applyPattern(sFormat);
    }
    return nf.format(value);
    */
  	
  	String	res = String.valueOf( value );
  	
  	int	pos = res.indexOf('.');
  	
  	if ( pos == -1 ){
  		
  		if ( precision != 0 ){
  			
	  		res += ".";
	  		
	  		for (int i=0;i<precision;i++){
	  		
	  			res += '0';
	  		}
  		}
  	}else{
  		
  		if ( precision == 0 ){
  			
  			res = res.substring(0,pos);
  			
  		}else{
  			
	  		int	digits = res.length() - pos - 1;
	  		
	  		if ( digits < precision ){
	  			
		  		for (int i=0;i<precision-digits;i++){
			  	
			  		res += '0';
			  	}
	  			
	  		}else if ( digits > precision ){
	  		
	  			res = res.substring( 0, pos+1+precision );
	  		}
  		}
  	}
  	
  	return( res );
  }
  
  		/**
  		 * Attempts vaguely smart string truncation by searching for largest token and truncating that
  		 * @param str
  		 * @param width
  		 * @return
  		 */
  
  	public static String
	truncateString(
		String	str,
		int		width )
  	{
  		int	excess = str.length() - width;
  		
  		if ( excess <= 0 ){
  			
  			return( str );
  		}
  		
  		excess += 3;	// for ...
  		
  		int	token_start = -1;
  		int	max_len		= 0;
  		int	max_start	= 0;
  		
  		for (int i=0;i<str.length();i++){
  			
  			char	c = str.charAt(i);
  			
  			if ( Character.isLetterOrDigit( c ) || c == '-' || c == '~' ){
  				
  				if ( token_start == -1 ){
  					
  					token_start	= i;
  					
  				}else{
  					
  					int	len = i - token_start;
  					
  					if ( len > max_len ){
  						
  						max_len		= len;
  						max_start	= token_start;
  					}
  				}
  			}else{
  				
  				token_start = -1;
  			}
  		}
  		
  		if ( max_len >= excess ){
  			 			
  			int	trim_point = max_start + max_len;
  			
  			return( str.substring( 0, trim_point - excess ) + "..." + str.substring( trim_point ));
  		}else{
  			
  			return( str.substring( 0, width-3 ) + "..." );
  		}
  	}
}