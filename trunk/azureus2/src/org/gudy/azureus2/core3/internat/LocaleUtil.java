package org.gudy.azureus2.core3.internat;

import java.io.UnsupportedEncodingException;

import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.*;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.torrent.*;
import org.gudy.azureus2.core3.util.*;

public abstract class
LocaleUtil 
	implements ILocaleUtilChooser 
{
  
  private static final String systemEncoding = System.getProperty("file.encoding");
  
  private static final String[] manual_charset = {
	systemEncoding,	// must be first entry due to code below that gets the system decoder
	"Big5","EUC-JP","EUC-KR","GB18030","GB2312","GBK","ISO-2022-JP","ISO-2022-KR",
	"Shift_JIS","KOI8-R",Constants.DEFAULT_ENCODING,"windows-1251",Constants.BYTE_ENCODING 
  };
  
	// the general ones *must* also be members of the above manual ones
  	
  protected static final String[] generalCharsets = {
	Constants.BYTE_ENCODING, Constants.DEFAULT_ENCODING, systemEncoding
  };
  
   private static LocaleUtilDecoder[] 	charsetDecoders;
   private static LocaleUtilDecoder		system_decoder;
   
  static {
  	
	List	decoders 		= new ArrayList();
  	List	decoder_names	= new ArrayList();
  	
	for (int i = 0; i < manual_charset.length; i++) {
	   try {
		 String	name = manual_charset[i];
		 
		 CharsetDecoder decoder = Charset.forName(name).newDecoder();
		 
		 decoder_names.add( name );
		 
		 LocaleUtilDecoder	lu_decoder =  new LocaleUtilDecoderReal(decoder, name);
		 
		 if ( i == 0 ){
		 	
		 	system_decoder = lu_decoder;
		 }
		 
		 decoders.add( lu_decoder );
		 		 
	   }catch (Exception ignore) {
	   }
	 }
	

	boolean show_all = COConfigurationManager.getBooleanParameter("File.Decoder.ShowAll" );

	if ( show_all ){
		
		Map m = Charset.availableCharsets();
	  	
		Iterator it = m.keySet().iterator();
	
		while(it.hasNext()){
	  		
			String	charset_name = (String)it.next();
	  		
			if ( !decoder_names.contains( charset_name)){
	  		
				try {
				  CharsetDecoder decoder = Charset.forName(charset_name).newDecoder();
				 
				  decoders.add( new LocaleUtilDecoderReal(decoder, charset_name));
				  
				  decoder_names.add( charset_name );
				 
				} catch (Exception ignore) {
				}
			}
		}
	}
    
	decoders.add( new LocaleUtilDecoderFallback());

	charsetDecoders	= new LocaleUtilDecoder[ decoders.size()];
	
	decoders.toArray( charsetDecoders);
  }
  
  public static String
  getSystemEncoding()
  {
  	return( systemEncoding );
  }
  
  public static LocaleUtilDecoder[]
  getDecoders()
  {
  	return( charsetDecoders );
  }
 
  private LocaleUtilDecoder lastChosenDecoder = null;
   
  private static ILocaleUtilChooser chooser = null;
    
  public static void setLocaleUtilChooser(ILocaleUtilChooser ch) {
	chooser=ch;
  }
   
  public LocaleUtilDecoder
  getSystemDecoder()
  {
  	return( system_decoder );
  }
  
  public LocaleUtilDecoder
  getLastChosenDecoder()
  {
  	return( lastChosenDecoder );
  }
  
  protected void
  setLastChosenDecoder(
  	LocaleUtilDecoder	d )
  {
  	lastChosenDecoder	= d;
  }
  
  protected static Candidate[] 
  getCandidates(
	byte[] array ) 
  {
	Candidate[] candidates = new Candidate[charsetDecoders.length];
    
	boolean show_less_likely_conversions = COConfigurationManager.getBooleanParameter("File.Decoder.ShowLax" );

	for (int i = 0; i < charsetDecoders.length; i++){
    	
	  candidates[i] = new Candidate(i);
      
	  try{
			LocaleUtilDecoder decoder = charsetDecoders[i];
      	      	
			String str = decoder.tryDecode( array, show_less_likely_conversions );

			if ( str != null ){
				
				candidates[i].value = str;
        		
				candidates[i].decoder = decoder;
			}
	  } catch (Exception ignore) {
      	
	  }
	}
    
    /*
	System.out.println( "getCandidates: = " + candidates.length );
	
	for (int i=0;i<candidates.length;i++){
		
		Candidate	cand = candidates[i];
		
		if ( cand != null ){
		
			String	value = cand.getValue();
			
			if ( value != null ){
			
				System.out.println( cand.getDecoder().getName() + "/" + (value==null?-1:value.length()) + "/" + value );
			}
		}  
	}
	*/
	
	return candidates;
  }
  
  protected static class 
  Candidate 
	implements Comparable 
  {
	private int					index;
	private String 				value;
	private LocaleUtilDecoder	decoder;
    
	protected
	Candidate(
		int	_index )
	{
		index	= _index;
	}
	
	public String getValue() {
	  return value;
	}
    
	public LocaleUtilDecoder getDecoder() {
	  return decoder;
	}
    
	public int 
	compareTo(Object o) 
	{
	  Candidate candidate = (Candidate)o;
      
	  int	res;
      
	  if( value == null && candidate.value == null){
      
		res	= 0;
        
	  }else if ( value == null ){
      	
		res = 1;
        
	  }else if ( candidate.value == null ){
      	
		res = -1;
      	
	  }else{
      
		res = value.length() - candidate.value.length();
        
		if ( res == 0 ){
        	
			res = index - candidate.index;
		}
	  }
      
	  // System.out.println( "comp:" + this.name + "/" + candidate.name + " -> " + res );
      
	  return( res );
	}
	/**
	 * only used for contains()
	 * Warning: this implementation covers not all special cases
	 */
	public boolean equals(Object obj) {
	  Candidate other = (Candidate) obj;
     
	 if ( value == null && other.value == null ){
		return( true );
	 }else if ( value == null || other.value == null ){
		return( false );
	 }else{
		return( value.equals( other.value ));
	 }
	}
  }
  
  
  public String 
  getChoosableCharsetString(
  	byte[] 		array,
	Object		decision_owner)
  
  	throws UnsupportedEncodingException 
  {
	throw new UnsupportedEncodingException("Hello, this is your base class speaking. You need to implement an ILocaleUtilChooser interface. This method is abstract here.");
  }

  
  public 
  LocaleUtil() 
  {
  }
  
  
  public static LocaleUtilDecoder
  getTorrentEncodingIfAvailable(
  		TOTorrent		torrent )
  
  throws TOTorrentException, UnsupportedEncodingException
  {
  	String	encoding = torrent.getAdditionalStringProperty( "encoding" );
  	
  	if ( encoding != null ){
  		
  		for (int i=0;i<charsetDecoders.length;i++){
  			
  			if ( charsetDecoders[i].getName().equals( encoding )){
  				
  				return( charsetDecoders[i] );
  			}
  		}
  	}
  	
  	return( null );
  }
  	
	public static LocaleUtilDecoder
	getTorrentEncoding(
  		TOTorrent		torrent )
  		
  		throws TOTorrentException, UnsupportedEncodingException
  	{
		String	encoding = torrent.getAdditionalStringProperty( "encoding" );
		
		if ( encoding != null ){
			
			for (int i=0;i<charsetDecoders.length;i++){
				
				if ( charsetDecoders[i].getName().equals( encoding )){
					
					return( charsetDecoders[i] );
				}
			}
		}
		
		LocaleUtil lut = chooser.getProperLocaleUtil();
		
		lut.lastChosenDecoder = null;
		
		lut.getChoosableCharsetString( torrent.getName(), torrent );
		
		if ( lut.lastChosenDecoder == null ){
			
			TOTorrentFile[]	files = torrent.getFiles();
			
			for (int i=0;i<files.length;i++){
				
				TOTorrentFile	file = files[i];
				
				byte[][] comps = file.getPathComponents();
				
				for (int j=0;j<comps.length;j++){
					
					lut.getChoosableCharsetString( comps[j], torrent );
					
					if ( lut.lastChosenDecoder != null ){
						
						break;
					}
				}
				
				if ( lut.lastChosenDecoder != null ){
					
					break;
				}
			}
		}
		
		if ( lut.lastChosenDecoder == null ){

			byte[]	comment = torrent.getComment();
			
			if ( comment != null ){
				
				lut.getChoosableCharsetString(comment, torrent);
			}
		}
		if ( lut.lastChosenDecoder == null ){

			byte[]	created = torrent.getCreatedBy();
			
			if ( created != null ){
				
				lut.getChoosableCharsetString(created, torrent);
			}
		}
		
		if ( lut.lastChosenDecoder == null ){
			
				// no choices required, use system default
				
			lut.lastChosenDecoder = charsetDecoders[0];
		}
		        	
		torrent.setAdditionalStringProperty("encoding", lut.lastChosenDecoder.getName());
            
		TorrentUtils.writeToFile( torrent );
			
		return( lut.lastChosenDecoder );
  	}
	
	public static void
	setTorrentEncoding(
		TOTorrent		torrent,
		String			encoding )
	{
		torrent.setAdditionalStringProperty("encoding", encoding );
	}
	
	public static void
	setDefaultTorrentEncoding(
		TOTorrent		torrent )
	{
		setTorrentEncoding( torrent, Constants.DEFAULT_ENCODING );
	}
}