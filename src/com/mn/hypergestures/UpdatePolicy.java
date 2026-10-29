package com.mn.hypergestures;
import java.net.URI;
public final class UpdatePolicy {
 public static final String REPOSITORY="Mnshawaty/HyperGesture-Releases", PACKAGE="com.mn.hypergestures";
 public static boolean allowedNetworkUrl(String value){try{URI u=new URI(value);String h=u.getHost();return "https".equals(u.getScheme())&&u.getUserInfo()==null&&u.getPort()==-1&&("api.github.com".equals(h)||"github.com".equals(h)||"release-assets.githubusercontent.com".equals(h)||"objects.githubusercontent.com".equals(h));}catch(Exception e){return false;}}
 public static boolean isAssetUrl(String value){try{URI u=new URI(value);return allowedNetworkUrl(value)&&"github.com".equals(u.getHost())&&u.getFragment()==null&&u.getPath().startsWith("/"+REPOSITORY+"/releases/download/")&&!u.getPath().contains("..");}catch(Exception e){return false;}}
 public static boolean newer(long installed,long remote){return remote>installed&&remote>0;}
 public static boolean validDigest(String s){return s!=null&&s.matches("[a-fA-F0-9]{64}");}
}

