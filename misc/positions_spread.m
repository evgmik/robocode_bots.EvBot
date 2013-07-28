function [pos,tstamp,v] =  positions_spread(t, x0, v0 )
% calculate possible positions of the bot 
% for t - turns
% moving with initial velocity v0
    pos = x0;
    tstamp = t;
    v=v0;
    if t <= 0
        return;
    end
    
    x0 = x0+v0;
    if t <=1
        pos =x0;
        return
    end

    t=t-1;
    if (v0 ~= 0)
        if abs(v0) < 8
            v_accel= v0+1*sign(v0);
            [posn,tnew,vnew]= positions_spread(t,x0, v_accel);
            pos=horzcat(pos, posn);
            tstamp = horzcat(tstamp, tnew);
            v=horzcat(v,vnew);
        end

        v_deaccel= v0-2*sign(v0);
        if ( v_deaccel*v0 < 1)
            v_deaccel =0;
        end
        [posn,tnew,vnew]= positions_spread(t,x0, v_deaccel);
        pos=horzcat(pos, posn);
        tstamp = horzcat(tstamp, tnew);
        v=horzcat(v,vnew);
    else
        v_accel= v0+1;
        [posn,tnew,vnew]= positions_spread(t,x0, v_accel);
        pos=horzcat(pos, posn);
        tstamp = horzcat(tstamp, tnew);
        v=horzcat(v,vnew);

        v_deaccel= v0-1;
        [posn,tnew,vnew]= positions_spread(t,x0, v_deaccel);
        pos=horzcat(pos, posn);
        tstamp = horzcat(tstamp, tnew);
        v=horzcat(v,vnew);
    end

    [posn,tnew,vnew]= positions_spread(t,x0, v0);
    pos=horzcat(pos, posn);
    tstamp = horzcat(tstamp, tnew);
    v=horzcat(v,vnew);
end


