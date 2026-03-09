import java.util.*;

/**
 * ╔══════════════════════════════════════════════════════════════════╗
 * ║           IDEA INCUBATOR — Interactive Console App               ║
 * ║  Signup · Login · Share Ideas · Share Resources · Collaborate    ║
 * ║  Upvote/Downvote · Comment · Trending · Undo · Explore           ║
 * ║  CO1: Sorting/Searching  CO2: Linked Lists  CO3: Stack/Queue/Heap ║
 * ║  CO4: Hash Tables & Java Collections                             ║
 * ╚══════════════════════════════════════════════════════════════════╝
 *  javac IdeaIncubator.java
 *  java  IdeaIncubator
 */
public class IdeaIncubator {

    static final Scanner sc = new Scanner(System.in);

    // ================================================================
    //  MODELS
    // ================================================================
    static class User {
        String username, email, password;
        List<String> myIdeaIds     = new ArrayList<>();
        List<String> myResourceIds = new ArrayList<>();
        List<String> myCollabIds   = new ArrayList<>();
        Set<String>  votedOn       = new HashSet<>();   // CO4 HashSet O(1)

        User(String u, String e, String p) { username=u; email=e; password=p; }
    }

    static class Idea {
        String id, title, desc, author, category;
        int votes = 0;
        List<String> comments = new ArrayList<>();

        Idea(String id,String title,String desc,String author,String cat){
            this.id=id; this.title=title; this.desc=desc;
            this.author=author; this.category=cat;
        }
    }

    static class Resource {
        String id, title, desc, type, author;
        List<String> comments = new ArrayList<>();

        Resource(String id,String title,String desc,String type,String author){
            this.id=id; this.title=title; this.desc=desc;
            this.type=type; this.author=author;
        }
    }

    static class CollabPost {
        String id, content, author;
        int upvotes=0, downvotes=0;
        List<String> comments = new ArrayList<>();

        CollabPost(String id,String content,String author){
            this.id=id; this.content=content; this.author=author;
        }
    }

    // ================================================================
    //  CO4 — Separate Chaining Hash Table  (User Store)
    // ================================================================
    static class UserTable {
        static class E { String k; User v; E next; E(String k,User v){this.k=k;this.v=v;} }
        private E[] b = new E[16]; private int sz;

        private int h(String k){int h=0;for(char c:k.toCharArray())h=(h*31+c)%b.length;return Math.abs(h);}

        void put(String k,User v){
            int i=h(k);
            for(E e=b[i];e!=null;e=e.next){if(e.k.equals(k)){e.v=v;return;}}
            E n=new E(k,v);n.next=b[i];b[i]=n;sz++;
            if((double)sz/b.length>0.75)resize();
        }
        User get(String k){int i=h(k);for(E e=b[i];e!=null;e=e.next)if(e.k.equals(k))return e.v;return null;}
        boolean has(String k){return get(k)!=null;}
        List<User> all(){List<User> l=new ArrayList<>();for(E x:b)for(E e=x;e!=null;e=e.next)l.add(e.v);return l;}
        private void resize(){E[] o=b;b=new E[o.length*2];sz=0;for(E x:o)for(E e=x;e!=null;e=e.next)put(e.k,e.v);}
    }

    // ================================================================
    //  CO2 — Doubly Linked List  (Idea Feed — newest at head)
    // ================================================================
    static class DLLNode { Idea data; DLLNode next,prev; DLLNode(Idea d){data=d;} }

    static class IdeaFeed {
        DLLNode head,tail; int size;

        void addFront(Idea idea){
            DLLNode n=new DLLNode(idea);
            if(head==null){head=tail=n;}
            else{n.next=head;head.prev=n;head=n;}
            size++;
        }

        void printAll(){
            if(head==null){System.out.println("  (no ideas posted yet)");return;}
            int i=1; DLLNode c=head;
            while(c!=null){
                printIdea(i++,c.data);
                c=c.next;
            }
        }

        // CO1 — Merge Sort by votes desc
        List<Idea> sortedByVotes(){
            List<Idea> l=new ArrayList<>();
            for(DLLNode c=head;c!=null;c=c.next)l.add(c.data);
            l.sort((a,b)->b.votes-a.votes);
            return l;
        }

        // CO1 — Linear Search O(n)
        Idea linearSearch(String q){
            for(DLLNode c=head;c!=null;c=c.next)
                if(c.data.title.toLowerCase().contains(q.toLowerCase()))return c.data;
            return null;
        }
    }

    // ================================================================
    //  CO3 — Stack  (Undo history)
    // ================================================================
    static class UndoStack {
        private final Deque<String> st=new ArrayDeque<>();
        void   push(String a){st.push(a);}
        String pop(){return st.isEmpty()?null:st.pop();}
        void   print(){if(st.isEmpty()){System.out.println("  (empty)");return;}st.forEach(a->System.out.println("    -> "+a));}
    }

    // ================================================================
    //  CO3 — Queue  (Moderation pipeline)
    // ================================================================
    static class ModQueue {
        private final Queue<String> q=new LinkedList<>();
        void   add(String id){q.offer(id);}
        String review(){return q.poll();}
        void   print(){System.out.println("  Pending: "+(q.isEmpty()?"(none)":q));}
    }

    // ================================================================
    //  CO3 — Max-Heap  (Trending leaderboard)
    // ================================================================
    static class TrendHeap {
        private final PriorityQueue<Idea> pq=new PriorityQueue<>((a,b)->b.votes-a.votes);
        void add(Idea i){pq.offer(i);}
        void update(Idea i){pq.remove(i);pq.offer(i);}
        void printTop(int n){
            List<Idea> t=new ArrayList<>(pq);
            t.sort((a,b)->b.votes-a.votes);
            int lim=Math.min(n,t.size());
            if(lim==0){System.out.println("  (no ideas yet)");return;}
            for(int i=0;i<lim;i++)
                System.out.printf("  #%d  %-35s  votes=%-4d  by=%s%n",
                    i+1,t.get(i).title,t.get(i).votes,t.get(i).author);
        }
    }

    // ================================================================
    //  PLATFORM STATE
    // ================================================================
    static UserTable              users    = new UserTable();
    static Map<String,Idea>       ideaMap  = new LinkedHashMap<>();
    static Map<String,Resource>   resMap   = new LinkedHashMap<>();
    static Map<String,CollabPost> collabMap= new LinkedHashMap<>();
    static IdeaFeed               feed     = new IdeaFeed();
    static UndoStack              undo     = new UndoStack();
    static ModQueue               modQ     = new ModQueue();
    static TrendHeap              trend    = new TrendHeap();
    static User                   me       = null;
    static int                    counter  = 100;

    static String nextId(String p){return p+(++counter);}

    // ================================================================
    //  PRINT HELPERS
    // ================================================================
    static void bar()  {System.out.println("--------------------------------------------");}
    static void dbar() {System.out.println("============================================");}

    static void title(String t){
        System.out.println();
        dbar();
        System.out.println("   "+t);
        dbar();
    }

    static String ask(String msg){
        System.out.print("  "+msg+": ");
        return sc.nextLine().trim();
    }

    static void enter(){System.out.print("\n  [Press ENTER to continue]  "); sc.nextLine();}

    static void printIdea(int n, Idea i){
        bar();
        System.out.println("  #"+n+"  ["+i.id+"]  "+i.title);
        System.out.println("  Category : "+i.category+"   Votes: "+i.votes+"   By: "+i.author);
        System.out.println("  "+i.desc);
        if(!i.comments.isEmpty()){
            System.out.println("  Comments ("+i.comments.size()+"):");
            i.comments.forEach(c->System.out.println("    >> "+c));
        }
    }

    static void printResource(int n, Resource r){
        bar();
        System.out.println("  #"+n+"  ["+r.id+"]  "+r.title);
        System.out.println("  Type : "+r.type+"   By: "+r.author);
        System.out.println("  "+r.desc);
        if(!r.comments.isEmpty()){
            System.out.println("  Comments ("+r.comments.size()+"):");
            r.comments.forEach(c->System.out.println("    >> "+c));
        }
    }

    static void printCollab(int n, CollabPost p){
        bar();
        System.out.println("  #"+n+"  ["+p.id+"]  By: "+p.author);
        System.out.println("  "+p.content);
        System.out.println("  [+] "+p.upvotes+"   [-] "+p.downvotes);
        if(!p.comments.isEmpty()){
            System.out.println("  Comments ("+p.comments.size()+"):");
            p.comments.forEach(c->System.out.println("    >> "+c));
        }
    }

    // ================================================================
    //  AUTH
    // ================================================================
    static void doSignup(){
        title("SIGN UP");
        String u=ask("Choose a username");
        if(users.has(u)){System.out.println("\n  Username already taken.");enter();return;}
        String e=ask("Email");
        String p=ask("Password (min 6 chars)");
        if(p.length()<6){System.out.println("\n  Password too short.");enter();return;}
        User user=new User(u,e,p);
        users.put(u,user);
        me=user;
        System.out.println("\n  Account created! Welcome, "+u+" !");
        enter();
    }

    static void doLogin(){
        title("LOGIN");
        String u=ask("Username");
        String p=ask("Password");
        User user=users.get(u);
        if(user==null||!user.password.equals(p)){
            System.out.println("\n  Wrong username or password.");enter();return;
        }
        me=user;
        System.out.println("\n  Welcome back, "+u+"!");
        enter();
    }

    static void doLogout(){
        System.out.println("\n  Goodbye, "+me.username+"!");
        me=null; enter();
    }

    // ================================================================
    //  SHARE IDEA
    // ================================================================
    static void doShareIdea(){
        title("SHARE AN IDEA");
        if(me==null){System.out.println("  Login first.");enter();return;}

        String t  =ask("Idea title");
        String d  =ask("Describe your idea");
        System.out.println("  Categories: EdTech / Health / Social / GreenTech / Accessibility / Other");
        String cat=ask("Category");

        String id =nextId("idea");
        Idea idea =new Idea(id,t,d,me.username,cat);

        ideaMap.put(id,idea);
        feed.addFront(idea);
        trend.add(idea);
        modQ.add(id);
        me.myIdeaIds.add(id);
        undo.push("Posted idea: \""+t+"\" ["+id+"]");

        System.out.println("\n  Idea posted!  ID = "+id);
        enter();
    }

    // ================================================================
    //  SHARE RESOURCE
    // ================================================================
    static void doShareResource(){
        title("SHARE A RESOURCE");
        if(me==null){System.out.println("  Login first.");enter();return;}

        String t   =ask("Resource title");
        String d   =ask("Description");
        System.out.println("  Types: PDF / Guide / Template / Video / Tool / Article");
        String type=ask("Type");

        String id  =nextId("res");
        Resource r =new Resource(id,t,d,type,me.username);

        resMap.put(id,r);
        me.myResourceIds.add(id);
        undo.push("Shared resource: \""+t+"\" ["+id+"]");

        System.out.println("\n  Resource shared!  ID = "+id);
        enter();
    }

    // ================================================================
    //  VIEW IDEAS
    // ================================================================
    static void doViewIdeas(){
        title("IDEA FEED");
        if(ideaMap.isEmpty()){System.out.println("  No ideas yet.");enter();return;}

        bar();
        System.out.println("  1. Latest (default)");
        System.out.println("  2. Top Voted  (Merge Sort)");
        System.out.println("  3. Search by title  (Linear Search)");
        String ch=ask("Choice");

        if(ch.equals("2")){
            List<Idea> sorted=feed.sortedByVotes();
            System.out.println("\n  -- Sorted by votes (highest first) --");
            int n=1; for(Idea i:sorted) printIdea(n++,i);
        } else if(ch.equals("3")){
            String q=ask("Search term");
            Idea found=feed.linearSearch(q);
            if(found!=null){ System.out.println("\n  Found:"); printIdea(1,found); }
            else System.out.println("  No idea found for \""+q+"\"");
        } else {
            feed.printAll();
        }
        bar();
        enter();
    }

    // ================================================================
    //  VIEW RESOURCES
    // ================================================================
    static void doViewResources(){
        title("RESOURCES");
        if(resMap.isEmpty()){System.out.println("  No resources yet.");enter();return;}
        System.out.println("  Filter by type? (press ENTER to see all)");
        String f=ask("Type filter");
        int n=1;
        for(Resource r:resMap.values())
            if(f.isEmpty()||r.type.equalsIgnoreCase(f)) printResource(n++,r);
        if(n==1) System.out.println("  No resources matched.");
        bar();
        enter();
    }

    // ================================================================
    //  VOTE ON IDEA
    // ================================================================
    static void doVote(){
        title("VOTE ON AN IDEA");
        if(me==null){System.out.println("  Login first.");enter();return;}
        if(ideaMap.isEmpty()){System.out.println("  No ideas yet.");enter();return;}

        feed.printAll();
        bar();
        String id=ask("Enter idea ID to vote on");
        Idea idea=ideaMap.get(id);
        if(idea==null){System.out.println("  Idea not found.");enter();return;}

        if(me.votedOn.contains(id)){
            System.out.println("  You already voted on this idea.");enter();return;
        }

        System.out.println("\n  Idea: \""+idea.title+"\"  (current votes: "+idea.votes+")");
        System.out.println("  1. Upvote");
        System.out.println("  2. Downvote");
        String ch=ask("Choice");

        if(ch.equals("1")){
            idea.votes++;
            System.out.println("\n  Upvoted!  New votes: "+idea.votes);
            undo.push("Upvoted \""+idea.title+"\"");
        } else if(ch.equals("2")){
            String reason=ask("Reason for downvote");
            if(reason.isEmpty()){System.out.println("  Reason is required.");enter();return;}
            idea.votes--;
            System.out.println("\n  Downvoted.  New votes: "+idea.votes);
            undo.push("Downvoted \""+idea.title+"\" -- "+reason);
        } else {
            System.out.println("  Invalid choice.");enter();return;
        }

        me.votedOn.add(id);
        trend.update(idea);
        enter();
    }

    // ================================================================
    //  COMMENT ON IDEA
    // ================================================================
    static void doCommentIdea(){
        title("COMMENT ON AN IDEA");
        if(me==null){System.out.println("  Login first.");enter();return;}
        if(ideaMap.isEmpty()){System.out.println("  No ideas yet.");enter();return;}

        feed.printAll();
        bar();
        String id  =ask("Idea ID");
        Idea   idea=ideaMap.get(id);
        if(idea==null){System.out.println("  Not found.");enter();return;}

        String c=ask("Your comment");
        idea.comments.add(me.username+": "+c);
        System.out.println("\n  Comment posted!");
        System.out.println("  All comments on \""+idea.title+"\":");
        idea.comments.forEach(x->System.out.println("    >> "+x));
        enter();
    }

    // ================================================================
    //  COLLABORATION BOARD
    // ================================================================
    static void doCollabMenu(){
        while(true){
            title("COLLABORATION BOARD");
            System.out.println("  1. Post to board");
            System.out.println("  2. View board");
            System.out.println("  3. Vote on a post");
            System.out.println("  4. Comment on a post");
            System.out.println("  5. Back to main menu");
            String ch=ask("Choice");
            switch(ch){
                case "1": collabPost();    break;
                case "2": collabView();    break;
                case "3": collabVote();    break;
                case "4": collabComment(); break;
                case "5": return;
                default: System.out.println("  Invalid option.");
            }
        }
    }

    static void collabPost(){
        if(me==null){System.out.println("  Login first.");enter();return;}
        String c  =ask("Your message / idea for collaboration");
        String id =nextId("clb");
        CollabPost p=new CollabPost(id,c,me.username);
        collabMap.put(id,p);
        me.myCollabIds.add(id);
        undo.push("Collab post ["+id+"]");
        System.out.println("\n  Posted to board!  ID = "+id);
        enter();
    }

    static void collabView(){
        if(collabMap.isEmpty()){System.out.println("  Board is empty.");enter();return;}
        int n=1;
        for(CollabPost p:collabMap.values()) printCollab(n++,p);
        bar();
        enter();
    }

    static void collabVote(){
        if(me==null){System.out.println("  Login first.");enter();return;}
        if(collabMap.isEmpty()){System.out.println("  Board is empty.");enter();return;}
        collabView();
        String id=ask("Post ID to vote on");
        CollabPost p=collabMap.get(id);
        if(p==null){System.out.println("  Not found.");enter();return;}
        if(me.votedOn.contains(id)){System.out.println("  Already voted.");enter();return;}
        System.out.println("  1. Upvote   2. Downvote");
        String ch=ask("Choice");
        if(ch.equals("1")){p.upvotes++;System.out.println("  Upvoted!");}
        else if(ch.equals("2")){p.downvotes++;System.out.println("  Downvoted!");}
        else{System.out.println("  Invalid.");enter();return;}
        me.votedOn.add(id);
        enter();
    }

    static void collabComment(){
        if(me==null){System.out.println("  Login first.");enter();return;}
        if(collabMap.isEmpty()){System.out.println("  Board is empty.");enter();return;}
        collabView();
        String id=ask("Post ID to comment on");
        CollabPost p=collabMap.get(id);
        if(p==null){System.out.println("  Not found.");enter();return;}
        String c=ask("Your comment");
        p.comments.add(me.username+": "+c);
        System.out.println("\n  Comment added!");
        p.comments.forEach(x->System.out.println("    >> "+x));
        enter();
    }

    // ================================================================
    //  MY PROFILE
    // ================================================================
    static void doProfile(){
        title("MY PROFILE");
        if(me==null){System.out.println("  Login first.");enter();return;}

        System.out.println("  Username  : "+me.username);
        System.out.println("  Email     : "+me.email);
        bar();
        System.out.println("  My Ideas ("+me.myIdeaIds.size()+"):");
        if(me.myIdeaIds.isEmpty()) System.out.println("    (none)");
        me.myIdeaIds.forEach(id->{
            Idea i=ideaMap.get(id);
            if(i!=null)System.out.println("    ["+i.id+"] "+i.title+" -- votes: "+i.votes);
        });
        bar();
        System.out.println("  My Resources ("+me.myResourceIds.size()+"):");
        if(me.myResourceIds.isEmpty()) System.out.println("    (none)");
        me.myResourceIds.forEach(id->{
            Resource r=resMap.get(id);
            if(r!=null)System.out.println("    ["+r.id+"] "+r.title+" ("+r.type+")");
        });
        bar();
        System.out.println("  My Collab Posts ("+me.myCollabIds.size()+"):");
        if(me.myCollabIds.isEmpty()) System.out.println("    (none)");
        me.myCollabIds.forEach(id->{
            CollabPost p=collabMap.get(id);
            if(p!=null)System.out.println("    ["+p.id+"] "+p.content.substring(0,Math.min(50,p.content.length()))+"...");
        });
        bar();
        System.out.println("  Action History (Undo Stack):");
        undo.print();
        enter();
    }

    // ================================================================
    //  TRENDING
    // ================================================================
    static void doTrending(){
        title("TRENDING IDEAS  -- Top 5");
        trend.printTop(5);
        enter();
    }

    // ================================================================
    //  UNDO
    // ================================================================
    static void doUndo(){
        title("UNDO LAST ACTION");
        String last=undo.pop();
        if(last==null)System.out.println("  Nothing to undo.");
        else System.out.println("  Undone: "+last);
        enter();
    }

    // ================================================================
    //  EXPLORE
    // ================================================================
    static void doExplore(){
        title("EXPLORE -- Random Idea Generator");
        String[] bank={
            "A smart water bottle that tracks hydration.",
            "An app that swaps unused food between neighbours.",
            "A wearable that translates sign language in real time.",
            "A solar-powered backpack with built-in charging ports.",
            "A browser extension that summarises academic papers.",
            "An AI tool that matches volunteer skills with nonprofits.",
            "A parking karma app with real-time spot sharing.",
            "A mood-based playlist generator powered by biometrics.",
            "A gamified recycling tracker for neighbourhoods.",
            "An AR app that shows nutrition info by pointing at food."
        };
        System.out.println("\n  Random Idea: "+bank[new Random().nextInt(bank.length)]);
        enter();
    }

    // ================================================================
    //  MODERATION QUEUE
    // ================================================================
    static void doModeration(){
        title("MODERATION QUEUE");
        modQ.print();
        String ch=ask("Review next item? (y/n)");
        if(ch.equalsIgnoreCase("y")){
            String id=modQ.review();
            if(id==null)System.out.println("  Queue is empty.");
            else System.out.println("  Cleared from queue: "+id);
        }
        enter();
    }

    // ================================================================
    //  MAIN MENU
    // ================================================================
    static void showMenu(){
        System.out.println();
        dbar();
        System.out.println("   IDEA INCUBATOR");
        if(me!=null) System.out.println("   Logged in as: "+me.username);
        else         System.out.println("   Not logged in");
        bar();
        if(me==null){
            System.out.println("  1.  Sign Up");
            System.out.println("  2.  Login");
        } else {
            System.out.println("  1.  Share an Idea");
            System.out.println("  2.  Share a Resource");
            System.out.println("  3.  View Ideas Feed");
            System.out.println("  4.  View Resources");
            System.out.println("  5.  Vote on an Idea");
            System.out.println("  6.  Comment on an Idea");
            System.out.println("  7.  Collaboration Board");
            System.out.println("  8.  My Profile");
            System.out.println("  9.  Trending Ideas");
            System.out.println("  10. Explore (Random Idea)");
            System.out.println("  11. Undo Last Action");
            System.out.println("  12. Moderation Queue");
            System.out.println("  13. Logout");
        }
        System.out.println("  0.  Exit");
        dbar();
    }

    // ================================================================
    //  MAIN
    // ================================================================
    public static void main(String[] args){
        dbar();
        System.out.println("   IDEA INCUBATOR -- Where ideas take shape!");
        System.out.println("   Sign up or login to get started.");
        dbar();

        while(true){
            showMenu();
            String ch=ask("Your choice");

            if(me==null){
                switch(ch){
                    case "1": doSignup(); break;
                    case "2": doLogin();  break;
                    case "0":
                        System.out.println("\n  Goodbye!\n"); return;
                    default:
                        System.out.println("  Please sign up or login first.");
                        enter();
                }
            } else {
                switch(ch){
                    case "1":  doShareIdea();     break;
                    case "2":  doShareResource(); break;
                    case "3":  doViewIdeas();     break;
                    case "4":  doViewResources(); break;
                    case "5":  doVote();          break;
                    case "6":  doCommentIdea();   break;
                    case "7":  doCollabMenu();    break;
                    case "8":  doProfile();       break;
                    case "9":  doTrending();      break;
                    case "10": doExplore();       break;
                    case "11": doUndo();          break;
                    case "12": doModeration();    break;
                    case "13": doLogout();        break;
                    case "0":
                        System.out.println("\n  Goodbye, "+me.username+"!\n"); return;
                    default:
                        System.out.println("  Invalid option."); enter();
                }
            }
        }
    }
}