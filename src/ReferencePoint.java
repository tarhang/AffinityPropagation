import java.util.*;

public class ReferencePoint
{
    public double x, y;
    public char orientation;
    public ReferencePoint examplar;
    public boolean examplar_changed;
    public boolean cluster_head;
    public ArrayList<Double> RSS;

    public ReferencePoint(double x, double y)
    {
        this.x = x;
        this.y = y;
        this.examplar = null;
        this.examplar_changed = true;
        this.cluster_head = false;
        RSS = new ArrayList<Double>();
    }

    public String toString()
    {
        String str = "(" + x + "," + y + ")";
        return str;
    }

    public boolean equals(ReferencePoint other)
    {
        if(this == other)
        {
            return true;
        }

        if(this == null || other == null)
        {
            return false;
        }

        if(this.getClass() != other.getClass())
        {
            return false;
        }

        if(this.x != other.x || this.y != other.y)
        {
            return false;
        }

        if(this.orientation != other.orientation)
        {
            return false;
        }

        for(int i = 0; i < this.RSS.size(); i += 1)
        {
            if(this.RSS.get(i) != other.RSS.get(i))
            {
                return false;
            }
        }

        return true;
    }
}
