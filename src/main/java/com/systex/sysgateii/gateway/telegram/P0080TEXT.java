package com.systex.sysgateii.gateway.telegram;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
/**
 * P0080TEXT
 * MatsudairaSyume
 *  20200123
 */
public class P0080TEXT {
	private static Logger log = LoggerFactory.getLogger(P0080TEXT.class);
    // P0080 TITA_TEXT
    private int  PBCNT = 6;  //!< 存摺本上可列印之資料筆數
    private int  BKSEQ = 1;  //!< 領用序號
    private int  REQCNT= 4;  //!< 要求輸出筆數
    private int  BEGIN = 4;  //!< 輸出起始位置

    private String  pbcnt = "pbcnt";
    private String  bkseq = "bkseq";
    private String  reqcnt = "reqcnt";    
    private String  begin = "begin";


    private int p0080titatext_lens[] = {
        PBCNT,  //!< 存摺本上可列印之資料筆數
        BKSEQ,  //!< 領用序號
        REQCNT,  //!< 要求輸出筆數
        BEGIN  //!< 輸出起始位置
    };

    private String p0080titatext_names[] = {
        pbcnt,
        bkseq,
        reqcnt,
        begin
    };

    // P0080 TOTA_TEXT
    private int DATE   = 8;   //!< 交易日期
    private int TRMNO  = 5;   //!< 櫃台機號
    private int TLRNO  = 2;   //!< 櫃員編號(併入櫃台機編號共7位)
    private int DSCPT  = 5;   //!< 交易摘要
    private int STXAMT = 1;   //!< 交易金額正負號
    private int TXAMT  = 13;  //!< 交易金額
    private int CRDB   = 1;   //!< 借貸記號
    private int DSPTYPE= 1;   //!< 備註種類
    private int DSPTKD = 3;   //!< 備註型式
    private int DSPTEXT= 18;  //!< 備註內容
    private int SPBBAL = 1;   //!< 存摺餘額正負號
    private int PBBAL  = 13;  //!< 存摺餘額


    private int NBCNT    = 3;  //!< 無摺資料總筆數
    private int TOTABKSEQ    = 1;  //!< 領用序號
    private int ENDFLG   = 1;  //!< 結束記號
    private int NBDELCNT = 2;  //!< 本次輸出筆數
    private int REQCN    = 4;  //!< 要求輸出筆數
    private int TOTABEGIN    = 4;  //!< 輸出起始位置
    private int OC       = 6;  //!< _nbdt_area oc=6

    private String date      = "date";   //!< 交易日期
    private String trmno    = "trmno";   //!< 櫃台機號
    private String tlrno    = "tlrno";   //!< 櫃員編號(併入櫃台機編號共7位)
    private String dscpt    = "dscpt";   //!< 交易摘要
    private String stxamt   = "stxamt";   //!< 交易金額正負號
    private String txamt    = "txamt";  //!< 交易金額
    private String crdb     = "crdb";   //!< 借貸記號
    private String dsptype  = "dsptype";   //!< 備註種類
    private String dsptkd   = "dsptkd";   //!< 備註型式
    private String dsptext  = "dsptext";  //!< 備註內容
    private String spbbal   = "spbbal";   //!< 存摺餘額正負號
    private String pbbal    = "pbbal";  //!< 存摺餘額


    private String nbcnt    = "nbcnt";  //!< 無摺資料總筆數
    private String totabkseq    = "totabkseq";  //!< 領用序號
    private String endflg   = "endflg";  //!< 結束記號
    private String nbdelcnt = "nbdelcnt";  //!< 本次輸出筆數
    private String reqcn    = "reqcn";  //!< 要求輸出筆數
    private String totabegin    = "totabegin";  //!< 輸出起始位置
    private String oc       =  "oc";  //!< _nbdt_area oc=6

    // _nbdtl_area  oc= 6;
    private int p0080totatext_lens[] = {
        DATE,
        TRMNO,
        TLRNO,
        DSCPT,
        STXAMT,
        TXAMT,
        CRDB,
        DSPTYPE,
        DSPTKD,
        DSPTEXT,
        SPBBAL,
        PBBAL,

        NBCNT,
        TOTABKSEQ,
        ENDFLG,
        NBDELCNT,
        REQCN,
        TOTABEGIN,
        OC
    };

    private String p0080totatext_names[] = {
        date,
        trmno,
        tlrno,
        dscpt,
        stxamt,
        txamt,
        crdb,
        dsptype,
        dsptkd,
        dsptext,
        spbbal,
        pbbal,
        nbcnt,
        totabkseq,
        endflg,
        nbdelcnt,
        reqcn,
        totabegin,
        oc
    };

    public void setValueLtoRfill(String fieldN, String setsV, byte fillV) throws Exception {
        setValueLtoRfill(fieldN, setsV.getBytes(), fillV);
    }
    public void setValueLtoRfill(String fieldN, byte[] setbV, byte fillV) throws Exception {
        Field f = p0080titatext.get(p0080titatextname.get(fieldN));
        byte[] ntb = null;
        if (setbV.length < f.len) {
//            int diff = f.len - setbV.length;
            ntb = new byte[f.len];
            Arrays.fill(ntb, fillV);
            System.arraycopy(setbV, 0, ntb, 0, setbV.length);
        } else
            ntb = setbV;
        setValue(fieldN, ntb);
        System.gc();
    }
    public void setValueRtoLfill(String fieldN, String setsV, byte fillV) throws Exception {
        setValueRtoLfill(fieldN, setsV.getBytes(), fillV);
    }
    public void setValueRtoLfill(String fieldN, byte[] setbV, byte fillV) throws Exception {
        Field f = p0080titatext.get(p0080titatextname.get(fieldN));
        byte[] ntb = null;
        if (setbV.length < f.len) {
            int diff = f.len - setbV.length;
            ntb = new byte[f.len];
            Arrays.fill(ntb, fillV);
            System.arraycopy(setbV, 0, ntb, diff, setbV.length);
        } else
            ntb = setbV;
        setValue(fieldN, ntb);
        System.gc();
    }

    public void setValue(String fieldN, String setsV) throws Exception {
        setValue(fieldN, setsV.getBytes());
    }
    public void setValue(String fieldN, byte[] setbV) throws Exception {
        Field f = p0080titatext.get(p0080titatextname.get(fieldN));
        System.arraycopy(setbV, 0, p0080titatextary, f.offset, f.len);
    }

    private List<Field> p0080titatext = new ArrayList<Field>();
    private Map<String, Integer> p0080titatextname = new HashMap<String, Integer>();
    private int p0080titatext_len = 0;
    private byte[] p0080titatextary = null;

    public P0080TEXT() {
        System.out.println("p0080titatext_lens of items=" + p0080titatext_lens.length);
        int id = 0;
        for (int l: p0080titatext_lens) {
            Field f = new Field();
//            log.debug(String.format("%d offset %d len %d",id, p0080titatext_len, l));
            f.setData(p0080titatext_names[id], p0080titatext_len, l);
            p0080titatextname.put(p0080titatext_names[id], id);
            id += 1;
            p0080titatext.add(f);
            p0080titatext_len += l;
        }
      log.debug("p0080titatext_lens={}",p0080titatext_len);

    }

    public boolean initP0080TitaTEXT(byte initValue) {
        if (p0080titatext_len > 0) {
            p0080titatextary = new byte[p0080titatext_len];
            Arrays.fill(p0080titatextary, initValue);
            return true;
        } else
            return false;
    }
    public byte[] getP0080Titatext() {
        return p0080titatextary;
    }

    public static void main(String[] args) throws Exception {
        boolean rtn;
        P0080TEXT tl = new P0080TEXT();
        rtn = tl.initP0080TitaTEXT((byte)'0');
        log.debug("tl.initP0080TitaTEXT rtn={}",rtn);
        byte[] result = tl.getP0080Titatext();
        //System.out.println("2--->" + Arrays.toString(result) + ":[" + new String(result) + "]");
        log.debug("2--->[{}]",new String(result));
    }
}