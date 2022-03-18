package msqueue;

import kotlinx.atomicfu.AtomicRef;

public class MSQueue implements Queue {
    private AtomicRef<Node> head;
    private AtomicRef<Node> tail;

    public MSQueue() {
        Node dummy = new Node(0);
        this.head = new AtomicRef<>(dummy);
        this.tail = new AtomicRef<>(dummy);
    }

    @Override
    public void enqueue(int x) {
        Node newTail = new Node(x);
        while(true){
            AtomicRef<Node> curTail = tail;
            AtomicRef<Node> next = curTail.getValue().next;
            if(next.compareAndSet(null, newTail)){
                tail.compareAndSet(curTail.getValue(), newTail);
                return;
            }
            else{
                tail.compareAndSet(curTail.getValue(), next.getValue());
            }
        }
    }

    @Override
    public int dequeue() {
        while(true){
            Node curHead = head.getValue();
            Node next = curHead.next.getValue();
            if(next == null){
                return Integer.MIN_VALUE;
            }
            if(head.compareAndSet(curHead, next)){
                return next.x;
            }
        }
    }

    @Override
    public int peek() {
        AtomicRef<Node> curHead = head;
        AtomicRef<Node> next = curHead.getValue().next;
        if (next.getValue() == null)
            return Integer.MIN_VALUE;
        return next.getValue().x;
    }

    private class Node {
        final int x;
        AtomicRef<Node> next;

        Node(int x) {
            this.x = x;
            next = new AtomicRef<>(null);
        }
    }
}