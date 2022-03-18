package linked_list_set;

import kotlinx.atomicfu.AtomicRef;

public class SetImpl implements Set {
    private class Node {
        AtomicRef<Node> next;
        int x;

        Node() {
            // empty.
        }

        Node(int x, Node next) {
            this.next = new AtomicRef<>(next);
            this.x = x;
        }

        Node checkNext() {
            Node node = next.getValue();
            if (node instanceof Removed)
                return ((Removed) node).next;
            else
                return node;
        }


    }
    private class Removed extends Node {
        final Node next;

        Removed(Node next) {
            this.next = next;
        }
    }

    private class Window {
        Node cur, next;
    }

    private final Node head = new Node(Integer.MIN_VALUE, new Node(Integer.MAX_VALUE, null));

    /**
     * Returns the {@link Window}, where cur.x < x <= next.x
     */
    private Window findWindow(int x) {
        while (true) {
            Window w = new Window();
            w.cur = head;
            w.next = w.cur.checkNext();
            Node nextNext;
            while (w.next.x < x) {
                nextNext = w.next.next.getValue();
                if (nextNext instanceof Removed) {
                    if (!w.cur.next.compareAndSet(w.next, ((Removed) nextNext).next)) {
                        w.cur = head;
                        w.next = w.cur.checkNext();
                    } else {
                        w.next = ((Removed) nextNext).next;
                    }
                } else {
                    w.cur = w.next;
                    w.next = w.cur.checkNext();
                }
            }
            nextNext = w.next.next.getValue();
            if (nextNext instanceof Removed) {
                w.cur.next.compareAndSet(w.next, ((Removed) nextNext).next);
            } else {
                return w;
            }
        }
    }

    @Override
    public boolean add(int x) {
        while (true){
            Window w = findWindow(x);
            if (w.next.x == x) {
                return false;
            }
            Node node = new Node(x, w.next);
            if (w.cur.next.compareAndSet(w.next, node)) {
                return true;
            }
        }
    }

    @Override
    public boolean remove(int x) {
        while (true) {
            Window w = findWindow(x);
            if (w.next.x != x) {
                return false;
            }
            Node nextNext = w.next.checkNext();
            Node nextRemoved = new Removed(nextNext);
            if (w.next.next.compareAndSet(nextNext, nextRemoved)) {
                w.cur.next.compareAndSet(w.next, nextNext);
                return true;
            }
        }
    }

    @Override
    public boolean contains(int x) {
        Window w = findWindow(x);
        boolean res = w.next.x == x;
        return res;
    }
}