import com.mn.hypergestures.SwipeDecision;
public class SwipeDecisionTest {
 static void eq(int actual,int expected,String name){if(actual!=expected)throw new AssertionError(name+": "+actual+" != "+expected);}
 public static void main(String[] args){
  SwipeDecision s=new SwipeDecision(100,800);s.move(100,700,100);eq(s.finish(100,680,150),1,"fast home");
  s=new SwipeDecision(100,800);s.move(100,700,100);eq(s.finish(102,698,500),2,"swipe then hold recents");
  s=new SwipeDecision(100,800);eq(s.finish(100,800,2000),0,"stationary long press is not home");
  s=new SwipeDecision(100,800);s.move(100,800,2000);s.move(100,700,2100);eq(s.finish(100,690,2200),1,"holding before swipe is not recents");
  s=new SwipeDecision(100,800);s.move(160,798,100);eq(s.finish(250,795,500),3,"horizontal switches");
  s=new SwipeDecision(100,800);s.move(100,690,100);s.cancel();eq(s.finish(100,690,600),0,"cancel cannot trigger");
  s=new SwipeDecision(100,800);s.move(100,760,100);eq(s.finish(100,760,600),0,"short swipe");
  s=new SwipeDecision(100,800);s.move(100,700,100);s.move(100,660,200);s.move(100,620,300);eq(s.finish(100,580,600),1,"continuous movement is home");
  s=new SwipeDecision(100,800);s.move(100,700,100);eq(s.finish(100,790,600),0,"return to bottom cancels action");
  s=new SwipeDecision(100,800);eq(s.finish(100,900,1000),0,"downward swipe");
  s=new SwipeDecision(100,800);s.move(125,800,100);eq(s.finish(130,800,200),0,"short horizontal rejected");
  s=new SwipeDecision(100,800);s.move(200,800,100);eq(s.finish(105,800,200),0,"horizontal returned to origin");
  s=new SwipeDecision(200,800);eq(s.finish(100,800,200),3,"left horizontal switches");
  s=new SwipeDecision(100,800);s.move(100,700,100);eq((int)s.recentsDelay(200),250,"hold timer remaining");
  s.move(100,650,300);eq((int)s.recentsDelay(400),250,"movement resets hold timer");
  s.move(100,790,500);eq((int)s.recentsDelay(600),-1,"return bottom cancels hold timer");
  s=new SwipeDecision(100,800);s.move(100,700,100);s.cancel();eq((int)s.recentsDelay(500),-1,"cancel disables timer");
  System.out.println("PASS: 17 gesture classification cases");
 }
}

