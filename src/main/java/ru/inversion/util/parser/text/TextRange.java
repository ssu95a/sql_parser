package ru.inversion.util.parser.text;

public final class TextRange {

    private final int start;
    private final int end;

    public TextRange(int start, int end) {
        if( start < 0 )
            throw new IllegalArgumentException("start < 0: " + start);
        if( end < start )
            throw new IllegalArgumentException( "end < start: [" + start + ", " + end + ")" );
        this.start = start;
        this.end = end;
    }

    public int start( ) { return start; }

    public int end() { return end; }

    public int length() { return end - start; }

    public boolean isEmpty() { return start == end; }
    /** */
    public boolean contains(int offset) {
        return start <= offset && offset < end;
    }

    @Override
    public String toString() {
        return "[" + start + ", " + end + ")";
    }
}