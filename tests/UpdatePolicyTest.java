import com.mn.hypergestures.UpdatePolicy;
public class UpdatePolicyTest {
 static int count;static void ok(boolean x){count++;if(!x)throw new AssertionError("case "+count);}
 public static void main(String[] args){
  String base="https://github.com/Mnshawaty/HyperGesture-Releases/releases/download/v0.4.4/";
  ok(UpdatePolicy.newer(13,14));ok(!UpdatePolicy.newer(14,14));ok(!UpdatePolicy.newer(15,14));ok(!UpdatePolicy.newer(0,-1));ok(UpdatePolicy.newer(14,3000000000L));
  ok(UpdatePolicy.isAssetUrl(base+"update.json"));ok(!UpdatePolicy.isAssetUrl(base.replace("https:","http:")+"a.apk"));ok(!UpdatePolicy.isAssetUrl(base.replace("github.com","github.com.evil.test")+"a.apk"));ok(!UpdatePolicy.isAssetUrl(base.replace("Mnshawaty/","someone/")+"a.apk"));ok(!UpdatePolicy.isAssetUrl(base+"../a.apk"));ok(!UpdatePolicy.isAssetUrl(base+"%2e%2e/a.apk"));ok(!UpdatePolicy.isAssetUrl(null));ok(!UpdatePolicy.isAssetUrl(base.replace("github.com","user@github.com")+"a.apk"));
  ok(UpdatePolicy.allowedNetworkUrl("https://release-assets.githubusercontent.com/file?signature=value"));ok(!UpdatePolicy.allowedNetworkUrl("https://release-assets.githubusercontent.com.evil.test/file"));ok(!UpdatePolicy.allowedNetworkUrl("file:///tmp/update.apk"));ok(!UpdatePolicy.allowedNetworkUrl("https://github.com:8443/file"));ok(UpdatePolicy.validDigest("abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789"));ok(!UpdatePolicy.validDigest("abcd"));ok(!UpdatePolicy.validDigest(null));
  System.out.println("PASS: "+count+" update policy cases");
 }
}

