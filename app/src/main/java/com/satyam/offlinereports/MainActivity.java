package com.satyam.offlinereports;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Local-first workbook browser/editor. No INTERNET permission or network calls. */
public class MainActivity extends Activity {
    private static final int PICK_IMPORT = 2101;
    private static final int PICK_EXPORT = 2102;
    private static final int PICK_EXPORT_CSV = 2103;
    private static final String PREFS = "local_account";
    private static final String DB_FILE = "workbook.json";
    private final int ink = Color.rgb(27, 44, 49), green = Color.rgb(18, 102, 83), muted = Color.rgb(103, 119, 123), bg = Color.rgb(245, 248, 247), line = Color.rgb(224, 233, 230), white = Color.WHITE;
    private LinearLayout root, body, header;
    private JSONObject workbook;
    private String currentSheet = "CSV", currentQuery = "";
    private int selectedRow = -1;
    private JSONArray sheetRows;
    private SharedPreferences prefs;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(10,49,44));
        getWindow().setNavigationBarColor(white);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        loadWorkbook();
        if (prefs.getString("passwordHash", null) == null) showSetPassword(); else showLogin();
    }

    private void loadWorkbook() {
        try {
            File f = new File(getFilesDir(), DB_FILE);
            if (f.exists()) workbook = new JSONObject(readAll(new FileInputStream(f)));
            else {
                workbook = new JSONObject(readAsset("workbook_seed.json"));
                saveWorkbook();
            }
        } catch (Exception e) { workbook = new JSONObject(); toast("Could not open local workbook: " + e.getMessage()); }
    }
    private String readAsset(String name) throws Exception { return readAll(getAssets().open(name)); }
    private String readAll(InputStream in) throws Exception {
        try (InputStream source = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192]; int n; while ((n = source.read(buf)) > 0) out.write(buf, 0, n);
            return out.toString("UTF-8");
        }
    }
    private void saveWorkbook() {
        try (FileOutputStream o = openFileOutput(DB_FILE, MODE_PRIVATE)) { o.write(workbook.toString().getBytes("UTF-8")); }
        catch (Exception e) { toast("Save failed: " + e.getMessage()); }
    }
    private String hash(String s) {
        try { byte[] b=MessageDigest.getInstance("SHA-256").digest(("SatyamOffline::"+s).getBytes("UTF-8")); StringBuilder x=new StringBuilder(); for(byte v:b)x.append(String.format(Locale.US,"%02x",v));return x.toString(); }
        catch(Exception e){return s;}
    }
    private void showSetPassword() {
        formShell("Set up local login", "Create a password for this device. Your password and report data stay on this phone.", "Create password", p -> {
            if (p.length() < 4) { toast("Use at least 4 characters."); showSetPassword(); return; }
            prefs.edit().putString("passwordHash", hash(p)).apply(); showHome();
        });
    }
    private interface PasswordAction { void run(String password); }
    private void formShell(String title, String hint, String button, PasswordAction action) {
        base(); LinearLayout panel = card(body); panel.setPadding(dp(22),dp(24),dp(22),dp(24));
        TextView mark = text("S", 24, white, true); mark.setGravity(Gravity.CENTER); mark.setBackground(round(green,16)); LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(dp(54),dp(54)); panel.addView(mark,mp);
        addText(panel,title,26,ink,true,dp(18)); addText(panel,hint,14,muted,false,dp(20));
        EditText input=new EditText(this);input.setSingleLine(true);input.setHint("Password");input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);input.setPadding(dp(14),0,dp(14),0);input.setBackground(round(white,12)); input.setTextColor(ink);input.setHintTextColor(muted); panel.addView(input,new LinearLayout.LayoutParams(-1,dp(54)));
        Button b=button(button); LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(52));bp.topMargin=dp(14);panel.addView(b,bp);
        b.setOnClickListener(v->{String p=input.getText().toString(); ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(input.getWindowToken(),0);action.run(p);});
    }
    private void showLogin() {
        formShell("Welcome back", "Satyam Agarbatti reports · Offline mode", "Unlock reports", p -> {
            if (hash(p).equals(prefs.getString("passwordHash", ""))) showHome(); else toast("Incorrect password.");
        });
    }

    private void base() {
        root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(bg);setContentView(root);
        ScrollView sc=new ScrollView(this);sc.setFillViewport(true);root.addView(sc,new LinearLayout.LayoutParams(-1,0,1));
        body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(18),dp(20),dp(18),dp(28));sc.addView(body);
    }
    private void showHome() {
        base();
        TextView brand=text("SATYAM  /  REPORTS",12,green,true); body.addView(brand);
        addText(body,"Offline workspace",28,ink,true,dp(5)); addText(body,"Your data stays on this device. No internet needed.",14,muted,false,dp(16));
        LinearLayout metricRow=new LinearLayout(this);metricRow.setOrientation(LinearLayout.HORIZONTAL);body.addView(metricRow,new LinearLayout.LayoutParams(-1,dp(100)));
        JSONArray sheets=sheets(); int totalRows=0; for(int i=0;i<sheets.length();i++) try {totalRows+=sheets.getJSONObject(i).getJSONArray("rows").length()-1;}catch(Exception ignored){}
        metric(metricRow,"SHEETS",String.valueOf(sheets.length())); metric(metricRow,"DATA ROWS",String.valueOf(Math.max(0,totalRows))); metric(metricRow,"STATUS","Offline");
        addText(body,"Your reports",19,ink,true,dp(18));
        addSheetTile("CSV", "Order-level source data", "Search, edit and maintain source rows");
        addSheetTile("Daily Report", "Daily summary", "Production report totals by date");
        addSheetTile("DET REPORT", "Detailed report", "Date and SKU totals");
        addSheetTile("SKU Master", "SKU / weight mapping", "Maintain patterns and weights");
        addSheetTile("Business Calendar", "Shift rules", "Business date adjustments and reasons");
        addSheetTile("tblMaster", "Master table", "Workbook master records");
        addText(body,"All workbook sheets",19,ink,true,dp(18));
        for(int i=0;i<sheets.length();i++)try{JSONObject sh=sheets.getJSONObject(i);String nm=sh.getString("name");if(!isPinned(nm))addSheetTile(nm,(sh.optBoolean("hidden")?"Hidden support sheet":"Workbook sheet"),rowCount(sh)+" rows · tap to browse");}catch(Exception ignored){}
        addText(body,"Data tools",19,ink,true,dp(18));
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.VERTICAL);body.addView(actions);
        actions.addView(button("Import workbook (.xlsx/.xlsm or JSON backup)",v->pickImport()));
        actions.addView(button("Export local data (.json backup)",v->exportJson()),buttonLp());
        actions.addView(button("Change local password",v->changePassword()),buttonLp());
        actions.addView(button("Lock app",v->showLogin()),buttonLp());
        addText(body,"Note: VBA macros do not run on Android. This app edits local worksheet data; formula/macro automation must be implemented separately where required.",12,muted,false,dp(20));
    }
    private boolean isPinned(String n){return n.equals("CSV")||n.equals("Daily Report")||n.equals("DET REPORT")||n.equals("SKU Master")||n.equals("Business Calendar")||n.equals("tblMaster");}
    private int rowCount(JSONObject sh){return Math.max(0,sh.optJSONArray("rows")==null?0:sh.optJSONArray("rows").length()-1);}
    private void metric(LinearLayout row,String label,String value){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(11),dp(13),dp(7),dp(10));box.setBackground(round(white,16));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-1,1);p.setMargins(0,0,dp(7),0);row.addView(box,p);addText(box,label,10,muted,true,0);addText(box,value,19,ink,true,dp(7));}
    private void addSheetTile(String sheet,String subtitle,String detail){LinearLayout c=card(body); c.setPadding(dp(15),dp(12),dp(14),dp(12)); LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,-2);cp.topMargin=dp(8);c.setLayoutParams(cp);LinearLayout r=new LinearLayout(this);r.setGravity(Gravity.CENTER_VERTICAL);r.setOrientation(LinearLayout.HORIZONTAL);c.addView(r);LinearLayout tx=new LinearLayout(this);tx.setOrientation(LinearLayout.VERTICAL);r.addView(tx,new LinearLayout.LayoutParams(0,-2,1));addText(tx,sheet,16,ink,true,0);addText(tx,subtitle,12,muted,false,dp(3));addText(tx,detail,12,green,false,dp(5));TextView arrow=text("›",28,green,false);r.addView(arrow);c.setOnClickListener(v->{currentSheet=sheet;currentQuery="";showSheet();});}
    private void showSheet() {
        base(); TextView back=text("‹  Dashboard",15,green,true);body.addView(back);back.setPadding(0,0,0,dp(12));back.setOnClickListener(v->showHome());
        JSONObject sh=findSheet(currentSheet);if(sh==null){toast("Sheet not found");showHome();return;}
        addText(body,currentSheet,25,ink,true,dp(5));JSONArray dataRows=sh.optJSONArray("rows");if(dataRows==null)dataRows=new JSONArray(); final JSONArray rows=dataRows; sheetRows=rows;
        addText(body,Math.max(0,rows.length()-1)+" data rows · saved on device",13,muted,false,dp(12));
        EditText search=new EditText(this);search.setSingleLine(true);search.setHint("Search in this sheet...");search.setText(currentQuery);search.setTextColor(ink);search.setHintTextColor(muted);search.setPadding(dp(13),0,dp(13),0);search.setBackground(round(white,12));body.addView(search,new LinearLayout.LayoutParams(-1,dp(50)));
        Button add=button("+ Add row",v->editRow(-1));body.addView(add,buttonLp());
        Button report=button("View sheet as table",v->showGrid(rows));body.addView(report,buttonLp());
        Button csv=button("Export this sheet as CSV",v->exportCsv());body.addView(csv,buttonLp());
        LinearLayout list=card(body);list.setPadding(dp(10),dp(8),dp(10),dp(8));LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.topMargin=dp(12);list.setLayoutParams(lp);
        populateRows(list,rows,currentQuery);
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int count){currentQuery=s.toString();populateRows(list,rows,currentQuery);}public void afterTextChanged(android.text.Editable e){}});
    }
    private void populateRows(LinearLayout list,JSONArray rows,String query){list.removeAllViews();if(rows.length()==0){addText(list,"No data",14,muted,false,dp(10));return;}JSONArray head=rows.optJSONArray(0);String q=query==null?"":query.toLowerCase(Locale.ROOT);int shown=0;
        for(int i=1;i<rows.length()&&shown<250;i++){final int rowIndex=i;JSONArray row=rows.optJSONArray(i);if(row==null)continue;StringBuilder all=new StringBuilder();for(int j=0;j<row.length();j++)all.append(" ").append(row.optString(j,""));if(!all.toString().toLowerCase(Locale.ROOT).contains(q))continue;
            LinearLayout item=new LinearLayout(this);item.setOrientation(LinearLayout.VERTICAL);item.setPadding(dp(8),dp(12),dp(8),dp(12));list.addView(item,new LinearLayout.LayoutParams(-1,-2));String primary=row.length()>0?row.optString(0,"Row "+i):"Row "+i;addText(item,"#"+i+"  "+primary,14,ink,true,0);
            StringBuilder meta=new StringBuilder();for(int j=1;j<Math.min(row.length(),5);j++){if(meta.length()>0)meta.append("   ·   ");String h=head!=null&&j<head.length()?head.optString(j,"Column "+(j+1)):"Column "+(j+1);meta.append(h).append(": ").append(row.optString(j,""));}addText(item,meta.toString(),12,muted,false,dp(5));item.setOnClickListener(v->editRow(rowIndex));View div=new View(this);div.setBackgroundColor(line);list.addView(div,new LinearLayout.LayoutParams(-1,dp(1)));shown++;
        }
        if(shown==0)addText(list,"No matches. Try another search.",14,muted,false,dp(12));else addText(list,"Showing "+shown+(shown>=250?"+":"")+" rows · tap a row to edit",11,muted,false,dp(10));
    }
    private int indexOfRow(JSONArray rows,JSONArray row){for(int i=1;i<rows.length();i++)if(rows.optJSONArray(i)==row)return i;return -1;}
    private void showGrid(JSONArray rows){JSONArray headerRow=rows.optJSONArray(0);if(headerRow==null){toast("No columns found");return;}ScrollView scroll=new ScrollView(this);HorizontalScrollView h=new HorizontalScrollView(this);LinearLayout table=new LinearLayout(this);table.setOrientation(LinearLayout.VERTICAL);h.addView(table);scroll.addView(h);
        LinearLayout hdr=new LinearLayout(this);hdr.setBackgroundColor(green);table.addView(hdr);for(int c=0;c<headerRow.length();c++)cell(hdr,headerRow.optString(c),true);
        for(int r=1;r<rows.length()&&r<=300;r++){JSONArray row=rows.optJSONArray(r);if(row==null)continue;LinearLayout rr=new LinearLayout(this);rr.setBackgroundColor(r%2==0?white:bg);table.addView(rr);for(int c=0;c<headerRow.length();c++)cell(rr,c<row.length()?row.optString(c,""):"",false);rr.setOnClickListener(v->editRow(indexOfRow(rows,row)));}
        new AlertDialog.Builder(this).setTitle(currentSheet+" · table preview").setView(scroll).setPositiveButton("Done",null).show();
    }
    private void cell(LinearLayout parent,String value,boolean head){TextView t=text(value,12,head?white:ink,head);t.setPadding(dp(10),dp(12),dp(10),dp(12));t.setMinWidth(dp(110));t.setMaxWidth(dp(240));t.setBackgroundColor(head?green:Color.TRANSPARENT);View v=new View(this);LinearLayout col=new LinearLayout(this);col.setOrientation(LinearLayout.VERTICAL);col.addView(t);parent.addView(col);}

    private void editRow(int index){JSONObject sh=findSheet(currentSheet);if(sh==null)return;JSONArray rows=sh.optJSONArray("rows");if(rows==null)return;JSONArray old=index>=0&&index<rows.length()?rows.optJSONArray(index):null;JSONArray header=rows.optJSONArray(0);if(header==null)return;
        ScrollView scroll=new ScrollView(this);LinearLayout fields=new LinearLayout(this);fields.setPadding(dp(18),dp(10),dp(18),dp(18));fields.setOrientation(LinearLayout.VERTICAL);scroll.addView(fields);ArrayList<EditText> inputs=new ArrayList<>();
        for(int c=0;c<header.length();c++){addText(fields,header.optString(c,"Column "+(c+1)),12,muted,true,dp(7));EditText e=new EditText(this);e.setSingleLine(false);e.setMinLines(1);e.setMaxLines(3);e.setTextColor(ink);e.setHintTextColor(muted);e.setPadding(dp(12),dp(7),dp(12),dp(7));e.setBackground(round(white,8));if(old!=null&&c<old.length())e.setText(old.optString(c,""));fields.addView(e,new LinearLayout.LayoutParams(-1,-2));inputs.add(e);}
        AlertDialog dlg=new AlertDialog.Builder(this).setTitle(index<0?"Add row":"Edit row #"+index).setView(scroll).setNegativeButton("Cancel",null).setNeutralButton(index>0?"Delete":"",null).setPositiveButton("Save",null).create();dlg.setOnShowListener(v->{dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(x->{try{JSONArray newer=new JSONArray();for(EditText e:inputs)newer.put(e.getText().toString());if(index<0)rows.put(newer);else rows.put(index,newer);sh.put("rows",rows);saveWorkbook();dlg.dismiss();toast("Saved on this device");showSheet();}catch(Exception ex){toast("Save failed: "+ex.getMessage());}});if(index>0)dlg.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(x->new AlertDialog.Builder(this).setTitle("Delete row?").setMessage("This removes row #"+index+" from the local workbook.").setNegativeButton("Cancel",null).setPositiveButton("Delete",(a,b)->{rows.remove(index);saveWorkbook();dlg.dismiss();toast("Row deleted");showSheet();}).show());});dlg.show();}

    private JSONObject findSheet(String name){JSONArray a=sheets();for(int i=0;i<a.length();i++){JSONObject o=a.optJSONObject(i);if(o!=null&&name.equals(o.optString("name")))return o;}return null;}
    private JSONArray sheets(){return workbook.optJSONArray("sheets")==null?new JSONArray():workbook.optJSONArray("sheets");}
    private void pickImport(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("*/*");i.putExtra(Intent.EXTRA_MIME_TYPES,new String[]{"application/json","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet","application/vnd.ms-excel.sheet.macroEnabled.12"});startActivityForResult(i,PICK_IMPORT);}
    private void exportJson(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/json");i.putExtra(Intent.EXTRA_TITLE,"satyam-reports-backup.json");startActivityForResult(i,PICK_EXPORT);}
    private void exportCsv(){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("text/csv");i.putExtra(Intent.EXTRA_TITLE,currentSheet.replaceAll("[^A-Za-z0-9_-]","_")+".csv");startActivityForResult(i,PICK_EXPORT_CSV);}
    @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data);if(res!=RESULT_OK||data==null||data.getData()==null)return;Uri u=data.getData();try{
        if(req==PICK_EXPORT){try(OutputStream o=getContentResolver().openOutputStream(u)){o.write(workbook.toString(2).getBytes("UTF-8"));}toast("Backup exported");}
        else if(req==PICK_EXPORT_CSV){JSONObject sh=findSheet(currentSheet);JSONArray rows=sh==null?new JSONArray():sh.optJSONArray("rows");try(OutputStream o=getContentResolver().openOutputStream(u)){if(o==null)throw new Exception("Could not open destination");if(rows!=null)for(int r=0;r<rows.length();r++){JSONArray row=rows.optJSONArray(r);if(row==null)continue;StringBuilder line=new StringBuilder();for(int c=0;c<row.length();c++){if(c>0)line.append(',');line.append(csvEscape(row.optString(c,"")));}line.append("\r\n");o.write(line.toString().getBytes("UTF-8"));}}toast("CSV exported");}
        else if(req==PICK_IMPORT){String name=getDisplayName(u);String lower=name==null?"":name.toLowerCase(Locale.ROOT);if(!(lower.endsWith(".json")||lower.endsWith(".xlsx")||lower.endsWith(".xlsm"))){toast("Choose a JSON backup, .xlsx, or .xlsm workbook.");return;}new AlertDialog.Builder(this).setTitle("Import data?").setMessage("This replaces the current local workbook. Export a JSON backup first if needed. Cached formula values can be imported, but VBA macros will not run on Android.").setNegativeButton("Cancel",null).setPositiveButton("Import",(d,w)->{try(InputStream in=getContentResolver().openInputStream(u)){if(in==null)throw new Exception("Could not read file");JSONObject incoming;if(lower.endsWith(".json")){incoming=new JSONObject(readAll(in));if(incoming.optJSONArray("sheets")==null)throw new Exception("This JSON is not a Satyam workbook backup.");}else{byte[] bytes=readBytes(in);incoming=parseExcelWorkbook(bytes,name);}workbook=incoming;saveWorkbook();currentSheet=findSheet("CSV")!=null?"CSV":(sheets().length()>0?sheets().optJSONObject(0).optString("name"):"CSV");toast("Imported "+sheets().length()+" sheets and saved locally");showHome();}catch(Exception ex){toast("Import failed: "+ex.getMessage());}}).show();}
    }catch(Exception e){toast("File operation failed: "+e.getMessage());}}
    private String csvEscape(String value){String v=value==null?"":value;if(v.contains(",")||v.contains("\"")||v.contains("\n")||v.contains("\r"))return "\""+v.replace("\"","\"\"")+"\"";return v;}
    private byte[] readBytes(InputStream in)throws Exception{try(InputStream src=in;ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=src.read(b))>0)out.write(b,0,n);return out.toByteArray();}}
    private JSONObject parseExcelWorkbook(byte[] bytes,String sourceName)throws Exception{
        Map<String,byte[]> files=new HashMap<>();try(ZipInputStream zis=new ZipInputStream(new ByteArrayInputStream(bytes))){ZipEntry e;while((e=zis.getNextEntry())!=null){if(!e.isDirectory()){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=zis.read(b))>0)out.write(b,0,n);files.put(e.getName(),out.toByteArray());}zis.closeEntry();}}
        Document wb=xml(files.get("xl/workbook.xml"));Document rel=xml(files.get("xl/_rels/workbook.xml.rels"));if(wb==null||rel==null)throw new Exception("File is not a supported Excel workbook.");Map<String,String> targets=new HashMap<>();NodeList relNodes=rel.getElementsByTagNameNS("*","Relationship");for(int i=0;i<relNodes.getLength();i++){Element e=(Element)relNodes.item(i);targets.put(e.getAttribute("Id"),e.getAttribute("Target"));}
        ArrayList<String> shared=new ArrayList<>();byte[] sb=files.get("xl/sharedStrings.xml");if(sb!=null){Document sd=xml(sb);NodeList sis=sd.getElementsByTagNameNS("*","si");for(int i=0;i<sis.getLength();i++)shared.add(sis.item(i).getTextContent());}
        JSONObject result=new JSONObject();result.put("sourceFile",sourceName==null?"Imported workbook":sourceName);result.put("generatedAt",new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",Locale.US).format(new Date()));JSONArray outSheets=new JSONArray();NodeList sheetNodes=wb.getElementsByTagNameNS("*","sheet");
        for(int i=0;i<sheetNodes.getLength();i++){Element se=(Element)sheetNodes.item(i);String name=se.getAttribute("name"),rid=se.getAttributeNS("http://schemas.openxmlformats.org/officeDocument/2006/relationships","id");if(rid.isEmpty())rid=se.getAttribute("r:id");String target=targets.get(rid);if(target==null)continue;String path=target.startsWith("/")?target.substring(1):(target.startsWith("xl/")?target:"xl/"+target);byte[] sheetBytes=files.get(path);if(sheetBytes==null)continue;Document doc=xml(sheetBytes);JSONArray rows=new JSONArray();NodeList rowNodes=doc.getElementsByTagNameNS("*","row");for(int r=0;r<rowNodes.getLength();r++){Element re=(Element)rowNodes.item(r);JSONArray row=new JSONArray();NodeList cells=re.getElementsByTagNameNS("*","c");for(int c=0;c<cells.getLength();c++){Element ce=(Element)cells.item(c);int ci=columnIndex(ce.getAttribute("r"));while(row.length()<=ci)row.put("");String type=ce.getAttribute("t"),value="";NodeList vals=ce.getElementsByTagNameNS("*","v");if(vals.getLength()>0)value=vals.item(0).getTextContent();if("s".equals(type)&&!value.isEmpty()){int ix=Integer.parseInt(value);value=ix>=0&&ix<shared.size()?shared.get(ix):"";}else if("inlineStr".equals(type)){NodeList ts=ce.getElementsByTagNameNS("*","t");if(ts.getLength()>0)value=ts.item(0).getTextContent();}else if("str".equals(type)){value=value;}row.put(ci,value);}rows.put(row);}
            if(rows.length()>0){JSONArray header=rows.optJSONArray(0);for(int rr=1;rr<rows.length();rr++){JSONArray row=rows.optJSONArray(rr);if(row==null)continue;for(int cc=0;cc<row.length()&&header!=null&&cc<header.length();cc++){String h=header.optString(cc,"").toLowerCase(Locale.ROOT),v=row.optString(cc,"");if(h.contains("date")&&!v.isEmpty()){try{double serial=Double.parseDouble(v);if(serial>15000&&serial<100000){long millis=(long)((serial-25569d)*86400000d);SimpleDateFormat fmt=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss",Locale.US);fmt.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));row.put(cc,fmt.format(new Date(millis)));}}catch(Exception ignored){}}}}}
            JSONObject one=new JSONObject();one.put("name",name);one.put("hidden",!se.getAttribute("state").isEmpty()&&se.getAttribute("state").equals("hidden"));one.put("rows",rows);outSheets.put(one);
        }
        if(outSheets.length()==0)throw new Exception("No worksheets could be read from this workbook.");result.put("sheets",outSheets);return result;
    }
    private Document xml(byte[] data)throws Exception{if(data==null)return null;DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();f.setNamespaceAware(true);try{f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);}catch(Exception ignored){}return f.newDocumentBuilder().parse(new ByteArrayInputStream(data));}
    private int columnIndex(String ref){int n=0;for(int i=0;i<ref.length();i++){char c=ref.charAt(i);if(c>='A'&&c<='Z')n=n*26+(c-'A'+1);else if(c>='a'&&c<='z')n=n*26+(c-'a'+1);else break;}return Math.max(0,n-1);}
    private String getDisplayName(Uri u){try(Cursor c=getContentResolver().query(u,null,null,null,null)){if(c!=null&&c.moveToFirst()){int n=c.getColumnIndex(OpenableColumns.DISPLAY_NAME);if(n>=0)return c.getString(n);}}catch(Exception ignored){}return null;}
    private void changePassword(){formShell("Change password","Enter a new local password. You will use it next time you open the app.","Save new password",p->{if(p.length()<4){toast("Use at least 4 characters");changePassword();return;}prefs.edit().putString("passwordHash",hash(p)).apply();toast("Password changed");showHome();});}

    private LinearLayout card(LinearLayout parent){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setBackground(round(white,16));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);parent.addView(c,p);return c;}
    private void addText(LinearLayout parent,String value,int size,int color,boolean bold,int top){TextView t=text(value,size,color,bold);if(top>0)t.setPadding(0,top,0,0);parent.addView(t,new LinearLayout.LayoutParams(-1,-2));}
    private TextView text(String value,int size,int color,boolean bold){TextView t=new TextView(this);t.setText(value);t.setTextSize(size);t.setTextColor(color);if(bold)t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setGravity(Gravity.CENTER_VERTICAL);return t;}
    private Button button(String value){return button(value,null);}
    private Button button(String value,View.OnClickListener listener){Button b=new Button(this);b.setText(value);b.setAllCaps(false);b.setTextSize(14);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);b.setTextColor(white);b.setBackground(round(green,12));b.setPadding(dp(12),0,dp(12),0);if(listener!=null)b.setOnClickListener(listener);return b;}
    private LinearLayout.LayoutParams buttonLp(){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,dp(50));p.topMargin=dp(8);return p;}
    private android.graphics.drawable.GradientDrawable round(int color,int radius){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
