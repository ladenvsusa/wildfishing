package com.rivertrace.angleratlas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RiverTraceView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final MainActivity host;
    private final int ink = Color.rgb(16, 45, 40);
    private final int paper = Color.rgb(244, 240, 231);
    private final int cream = Color.rgb(253, 250, 242);
    private final int orange = Color.rgb(240, 180, 90);
    private final int teal = Color.rgb(63, 126, 119);
    private int tab = 0;
    private float waterLevel = 20.62f;
    private float stationLevel = 20.62f;
    private float referenceMin = 18.62f;
    private float referenceMax = 21.62f;
    private String stationName = "汉口";
    private String stationTime = "05日 10时00分";
    private String hydrologyStatus = "正在同步长江水文网";
    private final List<CjhHydrologyClient.Station> stations = new ArrayList<>();
    private boolean stationPicker = false;
    private boolean spotDetail = false;
    private int historyImported = 0;
    private boolean draggingLevel = false;
    private boolean photoReady = false;
    private boolean saved = false;
    private int speciesIndex = 0;
    private int quantity = 3;
    private float weight = 1.8f;
    private final String[] species = {"鳊鱼", "鲫鱼", "鲤鱼", "翘嘴"};

    public RiverTraceView(Context context) {
        super(context);
        host = (MainActivity) context;
        p.setTypeface(Typeface.create("sans", Typeface.NORMAL));
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeWidth(dp(1));
        stroke.setColor(Color.argb(36, 16, 45, 40));
        setBackgroundColor(paper);
        seedStations();
        loadHydrology();
    }

    private float dp(float value) { return value * getResources().getDisplayMetrics().density; }
    private float sx(float value) { return value * getWidth() / 390f; }
    private float sy(float value) { return value * getHeight() / 844f; }
    private void color(int c) { p.setColor(c); p.setStyle(Paint.Style.FILL); }

    private void text(Canvas c, String value, float x, float y, float size, int color, boolean bold) {
        p.setColor(color); p.setTextSize(sx(size)); p.setStyle(Paint.Style.FILL);
        p.setTypeface(Typeface.create("sans", bold ? Typeface.BOLD : Typeface.NORMAL));
        c.drawText(value, sx(x), sy(y), p);
    }

    private void card(Canvas c, float l, float t, float rr, float b, float radius, int fill) {
        color(fill); r.set(sx(l), sy(t), sx(rr), sy(b)); c.drawRoundRect(r, sx(radius), sx(radius), p);
    }

    private void pill(Canvas c, String value, float l, float t, float rr, int fill, int textColor) {
        card(c, l, t, rr, t + 26, 13, fill);
        p.setTextSize(sx(11)); p.setTypeface(Typeface.create("sans", Typeface.BOLD));
        float width = p.measureText(value);
        text(c, value, (l + rr) / 2f - width / sx(2), t + 17, 11, textColor, true);
    }

    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        if (tab == 0) drawHome(c);
        else if (tab == 1) drawSpots(c);
        else if (tab == 2) drawLog(c);
        else if (tab == 3) drawHistory(c);
        else drawProfile(c);
        drawNav(c);
        if (stationPicker) drawStationPicker(c);
        if (spotDetail) drawSpotDetail(c);
    }

    private void drawHeader(Canvas c, String title, String sub) {
        color(ink); c.drawRect(0, 0, getWidth(), sy(104), p);
        text(c, "野水簿", 20, 34, 13, orange, true);
        text(c, title, 20, 67, 25, Color.WHITE, true);
        text(c, sub, 20, 90, 11, Color.rgb(190, 210, 204), false);
        pill(c, "武汉 · 晴", 292, 28, 372, Color.rgb(32, 66, 60), Color.WHITE);
    }

    private void drawHome(Canvas c) {
        drawHeader(c, "今天去哪儿钓？", "8月5日 周三 · 东南风 2级 · 29℃");
        text(c, "今日推荐", 20, 132, 13, ink, true);
        card(c, 16, 145, 374, 292, 22, ink);
        pill(c, "匹配 92%", 34, 163, 112, orange, ink);
        text(c, "府河 · 老闸口回水湾", 34, 208, 22, Color.WHITE, true);
        text(c, "退水 0.18m，缓流区更集中", 34, 233, 12, Color.rgb(190, 210, 204), false);
        pill(c, "鳊鱼", 34, 250, 84, Color.rgb(32, 66, 60), Color.WHITE);
        pill(c, "岸钓", 92, 250, 142, Color.rgb(32, 66, 60), Color.WHITE);
        pill(c, "35分钟", 150, 250, 214, Color.rgb(32, 66, 60), Color.WHITE);
        text(c, "›", 340, 226, 30, orange, true);

        text(c, "水位与河床", 20, 326, 13, ink, true);
        card(c, 16, 340, 374, 532, 22, cream);
        text(c, "定位武汉 → 参考 " + stationName + "  ·  点按切换", 34, 370, 12, ink, true);
        text(c, String.format(Locale.CHINA, "%.2f m", stationLevel), 34, 399, 24, teal, true);
        text(c, stationTime + "  ·  " + hydrologyStatus, 135, 389, 9, Color.rgb(93, 107, 101), false);
        text(c, String.format(Locale.CHINA, "地形参考 %.2fm（可手动调）", waterLevel), 135, 403, 9, orange, true);
        drawTerrain(c, 30, 414, 360, 486, waterLevel);
        float knobX = 42 + (waterLevel - referenceMin) / (referenceMax - referenceMin) * 306f;
        color(Color.rgb(218, 214, 203)); c.drawRoundRect(sx(42), sy(505), sx(348), sy(509), sx(2), sx(2), p);
        color(teal); c.drawRoundRect(sx(42), sy(505), sx(knobX), sy(509), sx(2), sx(2), p);
        color(orange); c.drawCircle(sx(knobX), sy(507), sx(8), p);
        text(c, String.format(Locale.CHINA, "%.1f", referenceMin), 38, 525, 9, Color.GRAY, false); text(c, "手动选择地形参考水位", 132, 525, 9, Color.GRAY, false); text(c, String.format(Locale.CHINA, "%.1f", referenceMax), 326, 525, 9, Color.GRAY, false);

        text(c, "出钓条件", 20, 566, 13, ink, true);
        card(c, 16, 580, 374, 664, 18, cream);
        text(c, "目标鱼", 34, 607, 10, Color.GRAY, false); text(c, "鳊鱼 / 鲫鱼", 34, 630, 14, ink, true);
        text(c, "可用时间", 163, 607, 10, Color.GRAY, false); text(c, "4 小时", 163, 630, 14, ink, true);
        text(c, "距离", 282, 607, 10, Color.GRAY, false); text(c, "≤ 40 km", 282, 630, 14, ink, true);
        card(c, 16, 680, 374, 728, 16, orange);
        text(c, "重新生成推荐", 139, 710, 14, ink, true);
    }

    private void drawTerrain(Canvas c, float l, float t, float rr, float b, float level) {
        Path bed = new Path();
        bed.moveTo(sx(l), sy(t + 18)); bed.cubicTo(sx(l + 50), sy(t + 25), sx(l + 70), sy(b - 18), sx(l + 120), sy(b - 24));
        bed.cubicTo(sx(l + 165), sy(b - 30), sx(l + 190), sy(t + 44), sx(l + 230), sy(t + 50));
        bed.cubicTo(sx(l + 275), sy(t + 58), sx(rr - 40), sy(t + 28), sx(rr), sy(t + 20));
        bed.lineTo(sx(rr), sy(b)); bed.lineTo(sx(l), sy(b)); bed.close();
        color(Color.rgb(199, 171, 128)); c.drawPath(bed, p);
        Path deep = new Path(bed); color(Color.rgb(162, 127, 87)); c.drawPath(deep, p);
        float normalized = Math.max(0, Math.min(1, (level - referenceMin) / (referenceMax - referenceMin)));
        float waterY = b - 28 - normalized * 39f;
        color(Color.argb(190, 63, 126, 119)); c.drawRect(sx(l), sy(waterY), sx(rr), sy(b), p);
        stroke.setColor(Color.rgb(180, 225, 219)); stroke.setStrokeWidth(sx(1));
        for (int i = 0; i < 4; i++) c.drawLine(sx(l), sy(waterY + 8 + i * 11), sx(rr), sy(waterY + 8 + i * 11), stroke);
        color(orange); c.drawCircle(sx(l + 108), sy(b - 18), sx(3), p); c.drawCircle(sx(l + 123), sy(b - 21), sx(2.5f), p);
        color(Color.WHITE); c.drawCircle(sx(l + 230), sy(waterY + 14), sx(2), p);
    }

    private void drawSpots(Canvas c) {
        drawHeader(c, "我的钓点", "12 个钓点 · 3 条水系");
        card(c, 16, 122, 374, 365, 22, Color.rgb(222, 230, 214));
        Path river = new Path(); river.moveTo(sx(8), sy(206)); river.cubicTo(sx(80), sy(154), sx(130), sy(306), sx(204), sy(236)); river.cubicTo(sx(272), sy(170), sx(315), sy(267), sx(389), sy(207));
        stroke.setStyle(Paint.Style.STROKE); stroke.setStrokeCap(Paint.Cap.ROUND); stroke.setStrokeWidth(sx(34)); stroke.setColor(Color.rgb(96, 154, 151)); c.drawPath(river, stroke);
        stroke.setStrokeWidth(sx(2)); stroke.setColor(Color.rgb(226, 245, 241)); c.drawPath(river, stroke);
        marker(c, 111, 225, "老闸口", true); marker(c, 222, 223, "桥下深坎", false); marker(c, 310, 223, "芦苇湾", false);
        pill(c, "地图为示意 · 接入高德后显示实景", 70, 330, 320, Color.argb(220, 253, 250, 242), ink);
        text(c, "附近钓点", 20, 398, 13, ink, true);
        spotCard(c, 16, 414, "府河 · 老闸口回水湾", "鳊鱼 · 鲫鱼 · 12次", "江滩停车场 · 步行620m", "8.6 km", true);
        spotCard(c, 16, 510, "汉江 · 蔡甸桥下深坎", "鲤鱼 · 翘嘴 · 7次", "桥西停车带 · 步行380m", "23 km", false);
        spotCard(c, 16, 606, "东荆河 · 芦苇湾", "鲫鱼 · 黄颡鱼 · 5次", "村口空地 · 步行1.1km", "31 km", false);
    }

    private void marker(Canvas c, float x, float y, String name, boolean selected) {
        color(selected ? orange : cream); c.drawCircle(sx(x), sy(y), sx(selected ? 13 : 10), p);
        color(ink); c.drawCircle(sx(x), sy(y), sx(4), p); text(c, name, x - 22, y - 19, 9, ink, true);
    }

    private void spotCard(Canvas c, float l, float t, String title, String fish, String parking, String distance, boolean fav) {
        card(c, l, t, 374, t + 82, 18, cream);
        text(c, fav ? "★" : "○", 32, t + 31, 15, fav ? orange : Color.LTGRAY, true);
        text(c, title, 58, t + 24, 14, ink, true); text(c, fish, 58, t + 46, 10, teal, true);
        text(c, "P  " + parking, 58, t + 67, 10, Color.GRAY, false); text(c, distance, 326, t + 48, 10, ink, true);
    }

    private void drawLog(Canvas c) {
        drawHeader(c, "记录这次渔获", "自动定位：府河 · 114.17°E, 30.65°N");
        text(c, "钓点名称", 20, 132, 11, Color.GRAY, false);
        card(c, 16, 145, 374, 192, 14, cream); text(c, "府河 · 老闸口回水湾 · " + new SimpleDateFormat("MM-dd HH:mm", Locale.CHINA).format(new Date()), 30, 174, 12, ink, true);
        text(c, "照片识别", 20, 222, 11, Color.GRAY, false);
        card(c, 16, 235, 374, 340, 18, photoReady ? Color.rgb(222, 235, 220) : Color.rgb(231, 228, 219));
        text(c, photoReady ? "✓" : "+", 42, 288, 30, photoReady ? teal : ink, true);
        text(c, photoReady ? "已完成本地 OCR 演示识别" : "选择渔获照片", 88, 275, 14, ink, true);
        text(c, photoReady ? "鳊鱼 3尾 · 约1.8kg · 置信度 86%" : "Demo 会填入可编辑的识别结果", 88, 302, 11, Color.GRAY, false);
        text(c, "识别结果（点击可修改）", 20, 372, 11, Color.GRAY, false);
        entry(c, 16, 386, "鱼种", species[speciesIndex], "点按切换");
        entry(c, 16, 452, "数量", quantity + " 尾", "点按 +1");
        entry(c, 16, 518, "总重", String.format(Locale.CHINA, "%.1f kg", weight), "点按 +0.2");
        card(c, 16, 590, 374, 649, 18, Color.rgb(226, 235, 232));
        text(c, "天气已同步", 34, 617, 11, teal, true); text(c, "晴 29℃ · 东南风2级 · 气压1003hPa", 34, 638, 11, ink, false);
        card(c, 16, 672, 374, 725, 17, saved ? teal : orange);
        text(c, saved ? "✓ 已保存到渔获记录" : "保存本次记录", saved ? 125 : 145, 705, 14, saved ? Color.WHITE : ink, true);
    }

    private void entry(Canvas c, float l, float t, String label, String value, String hint) {
        card(c, l, t, 374, t + 54, 14, cream); text(c, label, 32, t + 22, 10, Color.GRAY, false); text(c, value, 32, t + 43, 14, ink, true); text(c, hint, 295, t + 33, 9, teal, false);
    }

    private void drawHistory(Canvas c) {
        drawHeader(c, "出钓记录", "本月 4 次 · 总渔获 8.6 kg");
        card(c, 16, 126, 374, 210, 18, ink);
        text(c, "8.6", 34, 167, 27, Color.WHITE, true); text(c, "kg", 79, 167, 11, Color.LTGRAY, false);
        text(c, "本月总重", 34, 192, 10, Color.LTGRAY, false);
        text(c, "63%", 164, 167, 27, orange, true); text(c, "到钓率", 164, 192, 10, Color.LTGRAY, false);
        text(c, "4", 292, 167, 27, Color.WHITE, true); text(c, "次出钓", 292, 192, 10, Color.LTGRAY, false);
        text(c, "最近记录", 20, 243, 13, ink, true);
        historyCard(c, 16, 258, "今天 06:20", "府河 · 老闸口回水湾", "鳊鱼 3尾 · 1.8kg", "19.42m");
        historyCard(c, 16, 355, "8月2日 17:40", "汉江 · 蔡甸桥下深坎", "鲤鱼 1尾 · 2.4kg", "24.05m");
        historyCard(c, 16, 452, "7月29日 05:50", "东荆河 · 芦苇湾", "鲫鱼 9尾 · 1.6kg", "20.16m");
        card(c, 16, 566, 374, 619, 16, Color.rgb(226, 235, 232));
        text(c, historyImported > 0 ? "✓ 已导入 " + historyImported + " 个历史文件" : "上传历史鱼获（照片 / CSV）", historyImported > 0 ? 123 : 105, 598, 13, teal, true);
        text(c, "导入后仍可逐条校正鱼种、数量、重量和钓点", 68, 642, 10, Color.GRAY, false);
    }

    private void historyCard(Canvas c, float l, float t, String date, String place, String catchInfo, String water) {
        card(c, l, t, 374, t + 82, 17, cream); text(c, date, 30, t + 23, 10, teal, true); text(c, place, 30, t + 46, 14, ink, true);
        text(c, catchInfo, 30, t + 68, 10, Color.GRAY, false); pill(c, water, 309, t + 20, 360, Color.rgb(225, 235, 232), teal);
    }

    private void drawProfile(Canvas c) {
        drawHeader(c, "我的偏好", "推荐模型画像 · 仅保存在设备");
        card(c, 16, 126, 374, 215, 20, cream); text(c, "常钓鱼种", 32, 155, 11, Color.GRAY, false); pill(c, "鳊鱼", 32, 171, 88, orange, ink); pill(c, "鲫鱼", 96, 171, 152, Color.rgb(226,235,232), teal); pill(c, "鲤鱼", 160, 171, 216, Color.rgb(226,235,232), teal);
        text(c, "出钓习惯", 20, 250, 13, ink, true);
        entry(c, 16, 264, "常用钓法", "岸钓 · 手竿", "可编辑"); entry(c, 16, 330, "可接受距离", "40 km 内", "可编辑"); entry(c, 16, 396, "偏好时段", "05:00 — 10:00", "可编辑");
        text(c, "数据源状态", 20, 490, 13, ink, true);
        card(c, 16, 504, 374, 610, 18, cream); text(c, "水位", 32, 534, 12, ink, true); text(c, "长江水文网 · " + stationName, 240, 534, 10, teal, true); text(c, "天气", 32, 568, 12, ink, true); text(c, "演示数据源", 280, 568, 11, orange, true); text(c, "地形", 32, 601, 12, ink, true); text(c, "手工断面 + 参考水位", 236, 601, 10, teal, true);
    }

    private void drawStationPicker(Canvas c) {
        color(Color.argb(185, 8, 25, 22)); c.drawRect(0, 0, getWidth(), getHeight(), p);
        card(c, 16, 100, 374, 735, 24, cream);
        text(c, "选择参考水文站", 32, 133, 19, ink, true);
        text(c, "定位武汉时自动选择汉口，也可手动覆盖", 32, 153, 10, Color.GRAY, false);
        int count = Math.min(stations.size(), 14);
        for (int i = 0; i < count; i++) {
            CjhHydrologyClient.Station s = stations.get(i);
            float top = 168 + i * 39;
            if (s.name.equals(stationName)) card(c, 27, top - 8, 363, top + 29, 10, Color.rgb(226, 235, 232));
            text(c, s.name, 38, top + 10, 11, ink, true);
            text(c, s.river, 98, top + 10, 9, Color.GRAY, false);
            text(c, String.format(Locale.CHINA, "%.2fm", s.level), 276, top + 10, 11, teal, true);
            text(c, s.trendLabel(), 331, top + 10, 9, "上涨".equals(s.trendLabel()) ? Color.RED : teal, false);
        }
        text(c, "数据：长江水文网实时水情公开页面", 84, 720, 9, Color.GRAY, false);
    }

    private void drawSpotDetail(Canvas c) {
        color(Color.argb(185, 8, 25, 22)); c.drawRect(0, 0, getWidth(), getHeight(), p);
        card(c, 12, 126, 378, 744, 26, cream);
        text(c, "府河 · 老闸口回水湾", 30, 165, 20, ink, true); text(c, "×", 349, 164, 22, Color.GRAY, false);
        pill(c, "鳊鱼", 30, 182, 80, orange, ink); pill(c, "鲫鱼", 88, 182, 138, Color.rgb(226,235,232), teal); pill(c, "黄颡鱼", 146, 182, 207, Color.rgb(226,235,232), teal);
        text(c, "附近停车", 30, 236, 11, Color.GRAY, false); text(c, "府河江滩 3 号停车场", 30, 260, 15, ink, true); text(c, "免费 · 约 24 个车位", 30, 281, 10, teal, false);
        card(c, 26, 300, 364, 462, 18, Color.rgb(222,230,214));
        Path route = new Path(); route.moveTo(sx(67), sy(412)); route.cubicTo(sx(122), sy(378), sx(151), sy(437), sx(213), sy(382)); route.cubicTo(sx(247), sy(351), sx(279), sy(392), sx(326), sy(337));
        stroke.setStyle(Paint.Style.STROKE); stroke.setStrokeCap(Paint.Cap.ROUND); stroke.setStrokeWidth(sx(5)); stroke.setColor(teal); c.drawPath(route, stroke);
        color(ink); c.drawCircle(sx(67), sy(412), sx(9), p); color(orange); c.drawCircle(sx(326), sy(337), sx(10), p);
        text(c, "P", 63, 416, 9, Color.WHITE, true); text(c, "钓点", 311, 321, 9, ink, true);
        text(c, "推荐步行路线：沿防洪堤下行，避开泥滩", 42, 449, 10, ink, true);
        card(c, 26, 478, 364, 545, 16, ink); text(c, "620m", 45, 511, 20, Color.WHITE, true); text(c, "步行约 9 分钟", 128, 510, 11, Color.LTGRAY, false); text(c, "缓坡 · 少泥", 262, 510, 11, orange, true);
        text(c, "跑毒判断", 30, 580, 11, Color.GRAY, false);
        card(c, 26, 592, 364, 649, 15, Color.rgb(224, 238, 226)); text(c, "✓ 当前无需跑毒", 42, 618, 13, teal, true); text(c, "水位回落 · 无强降雨 · 返程缓冲 45 分钟", 42, 638, 9, ink, false);
        card(c, 26, 670, 364, 722, 16, orange); text(c, "开始步行导航", 137, 702, 14, ink, true);
    }

    private void drawNav(Canvas c) {
        color(cream); c.drawRect(0, sy(752), getWidth(), getHeight(), p);
        String[] labels = {"推荐", "钓点", "录入", "记录", "我的"};
        String[] icons = {"⌂", "●", "+", "≋", "○"};
        for (int i = 0; i < 5; i++) {
            float x = 39 + i * 78;
            if (i == 2) { color(i == tab ? orange : ink); c.drawCircle(sx(x), sy(778), sx(21), p); text(c, icons[i], x - 6, 786, 23, i == tab ? ink : Color.WHITE, false); }
            else { text(c, icons[i], x - 5, 782, 15, i == tab ? teal : Color.GRAY, true); }
            text(c, labels[i], x - 12, 812, 10, i == tab ? ink : Color.GRAY, i == tab);
        }
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX() * 390f / getWidth(); float y = e.getY() * 844f / getHeight();
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            if (stationPicker || spotDetail) return true;
            if (tab == 0 && y >= 490 && y <= 530) { draggingLevel = true; updateWater(x); return true; }
        } else if (e.getAction() == MotionEvent.ACTION_MOVE && draggingLevel) { updateWater(x); return true; }
        else if (e.getAction() == MotionEvent.ACTION_UP) {
            draggingLevel = false;
            if (stationPicker) {
                int index = (int)((y - 160) / 39);
                if (index >= 0 && index < Math.min(stations.size(), 14)) selectStation(stations.get(index));
                stationPicker = false; invalidate(); return true;
            }
            if (spotDetail) {
                if (y >= 665 && y <= 730) Toast.makeText(host, "步行导航为 Demo 示意，生产版接入高德步行路线", Toast.LENGTH_SHORT).show();
                else spotDetail = false;
                invalidate(); return true;
            }
            if (y >= 744) { tab = Math.max(0, Math.min(4, (int)(x / 78))); invalidate(); return true; }
            if (tab == 0 && y >= 340 && y <= 410) { stationPicker = true; invalidate(); return true; }
            if (tab == 1 && y >= 405 && y <= 700) { spotDetail = true; invalidate(); return true; }
            if (tab == 2) {
                if (y >= 230 && y <= 345) host.chooseCatchPhoto();
                else if (y >= 386 && y <= 440) { speciesIndex = (speciesIndex + 1) % species.length; invalidate(); }
                else if (y >= 452 && y <= 506) { quantity++; invalidate(); }
                else if (y >= 518 && y <= 572) { weight += .2f; invalidate(); }
                else if (y >= 666 && y <= 732) { saved = true; invalidate(); Toast.makeText(host, "记录已保存（演示）", Toast.LENGTH_SHORT).show(); }
            }
            if (tab == 3 && y >= 560 && y <= 625) { host.chooseHistoryFiles(); return true; }
            if (tab == 0 && y >= 670 && y <= 735) Toast.makeText(host, "已根据目标鱼、距离、水位和历史收获重新排序", Toast.LENGTH_SHORT).show();
            return true;
        }
        return true;
    }

    private void updateWater(float x) {
        float normalized = Math.max(0, Math.min(1, (x - 42f) / 306f));
        waterLevel = referenceMin + normalized * (referenceMax - referenceMin); invalidate();
    }

    private void seedStations() {
        long now = System.currentTimeMillis();
        stations.add(new CjhHydrologyClient.Station("60105400", "寸滩", "长江干流", 171.48, "26200", now, "4"));
        stations.add(new CjhHydrologyClient.Station("60803000", "武隆", "乌江", 176.08, "2660", now, "5"));
        stations.add(new CjhHydrologyClient.Station("60107300", "宜昌", "长江干流", 44.61, "18100", now, "4"));
        stations.add(new CjhHydrologyClient.Station("60108300", "沙市", "长江干流", 36.51, "19400", now, "4"));
        stations.add(new CjhHydrologyClient.Station("60111200", "莲花塘", "长江干流", 26.78, "-", now, "5"));
        stations.add(new CjhHydrologyClient.Station("60112200", "汉口", "长江干流", 20.62, "26400", now, "6"));
        stations.add(new CjhHydrologyClient.Station("60113400", "九江", "长江干流", 14.90, "26800", now, "4"));
        stations.add(new CjhHydrologyClient.Station("60115000", "大通", "长江干流", 9.86, "34500", now, "4"));
        stations.add(new CjhHydrologyClient.Station("61512000", "七里山", "洞庭湖湖口", 26.78, "7390", now, "5"));
        stations.add(new CjhHydrologyClient.Station("62601600", "湖口", "鄱阳湖", 14.32, "5910", now, "6"));
        stations.add(new CjhHydrologyClient.Station("60107000", "茅坪(二)", "长江", 155.01, "-", now, "5"));
        stations.add(new CjhHydrologyClient.Station("60106980", "三峡水库", "长江干流", 155.01, "16300出", now, "5"));
        stations.add(new CjhHydrologyClient.Station("61802500", "龙王庙", "汉江", 162.22, "-", now, "5"));
        stations.add(new CjhHydrologyClient.Station("61802700", "丹江口水库", "汉江", 162.22, "6000入/434出", now, "5"));
    }

    private void loadHydrology() {
        new CjhHydrologyClient().fetch(new CjhHydrologyClient.Callback() {
            @Override public void onLoaded(List<CjhHydrologyClient.Station> loaded) {
                stations.clear(); stations.addAll(loaded); hydrologyStatus = "长江水文网实时";
                String wanted = ReferenceStationResolver.forCity("武汉市");
                for (CjhHydrologyClient.Station station : stations) if (station.name.equals(wanted)) { selectStation(station); break; }
                invalidate();
            }

            @Override public void onError(String message) {
                hydrologyStatus = "网页暂不可用 · 使用缓存"; invalidate();
            }
        });
    }

    private void selectStation(CjhHydrologyClient.Station station) {
        stationName = station.name; stationLevel = (float) station.level; waterLevel = stationLevel;
        referenceMin = stationLevel - 2f; referenceMax = stationLevel + 1f;
        stationTime = new SimpleDateFormat("dd日 HH时mm分", Locale.CHINA).format(new Date(station.measuredAt));
    }

    public void onPhotoSelected(String uri) {
        photoReady = true; speciesIndex = 0; quantity = 3; weight = 1.8f; invalidate();
        Toast.makeText(host, "已生成可编辑的 OCR 演示结果", Toast.LENGTH_SHORT).show();
    }

    public void onHistoryFilesSelected(int count) {
        historyImported = count; invalidate();
        Toast.makeText(host, "已导入 " + count + " 个历史鱼获文件（演示）", Toast.LENGTH_SHORT).show();
    }
}
