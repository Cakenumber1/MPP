package stack;

import kotlinx.atomicfu.AtomicRef;

import java.util.Collections;
import java.util.List;

public class StackImpl implements Stack {
    private static class Node {
        final AtomicRef<Node> next;
        final int val;

        Node(int x, Node next) {
            this.next = new AtomicRef<>(next);
            this.val = x;
        }
    }

    // head pointer
    private AtomicRef<Node> head = new AtomicRef<>(null);

    // array
    private final int length = 10;
    private List<AtomicRef<Integer>> elimArray = Collections.nCopies(length,new AtomicRef<Integer>(null));
    private final int steps = 2;
    private final int max = length;
    private final int min = 0;

    @Override
    public void push(int a) {
        Integer x = a;
        int index = min + (int) (Math.random() * ((max - min)));
        for (int t = Math.max(index - steps, 0); t < Math.min(index + steps, length); t++) {
            if (elimArray.get(t).compareAndSet(null, x )) {
                int waittime = 20;
                int w = 0;
                while(w < waittime){
                    Integer cur = elimArray.get(t).getValue();
                    if (cur == null || !cur.equals(x) )
                        return;
                    w++;
                }
                if(elimArray.get(t).compareAndSet(x, null))
                    break;
                else{
                    return;
                }
            }
        }
            while (true) {
                Node curHead = head.getValue();
                AtomicRef<Node> newHead = new AtomicRef<>(new Node(x,curHead));
                if (head.compareAndSet(curHead, newHead.getValue())) {
                    return;
                }
            }
        }

    @Override
    public int pop() {

        int index = min + (int) (Math.random() * ((max - min)));

        for (int t = Math.max(index - steps, 0); t < Math.min(index + steps, length); t++) {
            Integer cur = elimArray.get(t).getValue();
            if (cur != null && elimArray.get(t).compareAndSet(cur,null )) {
                return cur;
            }
        }
        while (true) {
            Node curHead = head.getValue();
            if (curHead == null) return Integer.MIN_VALUE;
            if (head.compareAndSet(curHead, curHead.next.getValue()))
                return curHead.val;
        }

    }
}
