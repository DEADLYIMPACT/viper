package com.games.viper;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.google.gson.Gson;

import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

public class SnakeActivity extends Activity implements Runnable {

    private SurfaceView surfaceView;
    private Button pause;
    private TextView scoreView;
    private TextView scoreViewLobby;
    private TextView highScoreText;
    private Button start;
    private Button gameModeSelect;
    private Button quit;
    private Button stats;
    private Button star;
    private Button settings;
    private Button shop;
    private RelativeLayout rl;
    private LinearLayout bottom;
    private MediaPlayer backTrack;
    private MediaPlayer lobbyTrack;
    private MediaPlayer eat;
    private MediaPlayer open;
    private ImageView logo;
    private Locale locale;

    private static final String TAG = "FOOT";

    private int score;
    private int foodNo;
    private int snakeLength;
    private int FPS;
    private final int appleTime = 20000;
    private final int sappleTime = 15000;
    private final int gappleTime = 10000;
    private final int godappleTime = 7500;
    private int scoreAddX;
    private int scoreAddY;
    private int scoreAdd;
    private int alpha;
    private int scoreMilstone;
    private final int SCREEN_BLOCKS_WIDTH = 20;
    private final int SCREEN_BLOCKS_HEIGHT = 30;
    private final int[] gameModeNames = {R.string.classic, R.string.no_walls, R.string._3_x_food, R.string._5_x_food, R.string.practice};
    private int[] highScore;
    private int[] totalScore;
    private int[] gamesPlayed;
    private int[] snakeX;
    private int[] snakeY;
    private int[] scoreFood;
    private int[] foodX;
    private int[] foodY;
    private final int[][] skins = {{255, 0, 0}, {0, 255, 0}, {0, 0, 255}, {255, 255, 0}, {0, 255, 255}, {255, 0, 255},
            //                             red          green         blue         yellow         cyan          magenta
            {255, 125, 0}, {125, 0, 125}, {125, 62, 0}, {0, 0, 0}, {125, 125, 125}, {255, 255, 255}};
    //                          orange         violet         brown         black       gray            white
    private int selectedColor = 0;

    private long nextFrameTime;
    private long scoreAddTime;
    private long[] foodTime;
    private long prevTime;

    private float screenWidth;
    private float screenHeight;
    private float blockHeight;
    private float blockWidth;
    private float musicVolume = 0.5f;
    private float audioVolume = 0.5f;

    private boolean[] foodVisible;
    private volatile boolean isPlaying;
    private boolean dead;
    private boolean paused;
    private boolean hasChangedDirection;

    private Thread thread;

    private Bitmap apple;
    private Bitmap sapple;
    private Bitmap gapple;
    private Bitmap godapple;
    private Bitmap[] foodBitmap;

    private enum Heading {UP, DOWN, RIGHT, LEFT, STILL}

    private Heading heading = Heading.STILL;

    private int gameMode;

    private final Paint paint = new Paint();

    private GestureDetector detector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(R.layout.activity_main2);

        // Screen Values
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;
        Log.d(TAG, "onCreate: " + screenHeight);
        float screenPlayHeight = (float) SCREEN_BLOCKS_HEIGHT / SCREEN_BLOCKS_WIDTH * screenWidth;
        blockHeight = screenPlayHeight / SCREEN_BLOCKS_HEIGHT;
        blockWidth = screenWidth / SCREEN_BLOCKS_WIDTH;

        // Get High Scores and Saved Game Mode
        SharedPreferences sharedPreferences = getSharedPreferences("values", 0);
        Gson gson = new Gson();
        String hs = sharedPreferences.getString("hs", null);
        String ts = sharedPreferences.getString("ts", null);
        String gp = sharedPreferences.getString("gp", null);
        String lang = sharedPreferences.getString("lang", getResources().getConfiguration().locale.toString());
        highScore = new int[4];
        totalScore = new int[4];
        gamesPlayed = new int[4];
        if (hs == null) {
            highScore = new int[4];
            totalScore = new int[4];
            gamesPlayed = new int[4];
            Arrays.fill(highScore, 0);
            Arrays.fill(totalScore, 0);
            Arrays.fill(gamesPlayed, 0);
            if (lang.contains("es"))
                lang = "es";
            else if (lang.contains("ru"))
                lang = "ru";
            else if (lang.contains("pt"))
                lang = "pt";
            else if (lang.contains("fr"))
                lang = "fr";
            showInstructions();
        } else {
            highScore = gson.fromJson(hs, highScore.getClass());
            totalScore = gson.fromJson(ts, totalScore.getClass());
            gamesPlayed = gson.fromJson(gp, totalScore.getClass());
        }
        setLang(lang);
        Log.d(TAG, "onCreate: " + locale.toString());
        gameMode = sharedPreferences.getInt("gamemode", 0);
        selectedColor = sharedPreferences.getInt("color", 0);
        musicVolume = sharedPreferences.getFloat("musicVol", 0.5f);
        audioVolume = sharedPreferences.getFloat("audioVol", 0.5f);

        // Font
        Typeface typeface = ResourcesCompat.getFont(this, R.font.pixel2);
        paint.setTypeface(typeface);
        paint.setTextSize(screenPlayHeight / 40);

        // Bitmaps
        final Resources resources = this.getResources();
        apple = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources,
                R.drawable.apple), (int) blockWidth, (int) blockHeight, false);

        sapple = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources,
                R.drawable.sapple), (int) blockWidth, (int) blockHeight, false);

        gapple = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources,
                R.drawable.gapple), (int) blockWidth, (int) blockHeight, false);

        godapple = Bitmap.createScaledBitmap(BitmapFactory.decodeResource(resources,
                R.drawable.godapple), (int) blockWidth, (int) blockHeight, false);

        // Buttons
        start = findViewById(R.id.start);
        start.setText(getString(R.string.play));
        start.setOnClickListener(view -> game());

        pause = findViewById(R.id.pause);
        pause.setOnClickListener(view -> {
            if (isPlaying) {
                paused = true;
                pause.setBackgroundResource(R.drawable.play);
            } else {
                paused = false;
                resumeGame();
                pause.setBackgroundResource(R.drawable.pause);
            }
        });

        gameModeSelect = findViewById(R.id.gameMode);
        highScoreText = findViewById(R.id.highscore);
        gameModeSelect.setText(getString(gameModeNames[gameMode]));
        if (gameMode == 4) {
            highScoreText.setText("");
        } else {
            highScoreText.setText(String.format(getString(R.string.highscore) + " - %s", highScore[gameMode]));
        }
        gameModeSelect.setOnClickListener(view -> showGameModeDialog());

        stats = findViewById(R.id.stats);
        stats.setOnClickListener(view -> showStatsDialog(gameMode));

        star = findViewById(R.id.star);
        star.setOnClickListener(view -> {
            final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
            } catch (android.content.ActivityNotFoundException anfe) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
            }
        });
        settings = findViewById(R.id.settings);
        settings.setOnClickListener(view -> showSettings());
        shop = findViewById(R.id.shop);
        shop.setOnClickListener(view -> showShopDialog());

        quit = findViewById(R.id.quit);
        quit.setOnClickListener(view -> {
            if (paused) {
                paused = false;
                isPlaying = true;
                thread = new Thread(SnakeActivity.this);
                thread.start();
            }
            dead = true;
        });
        quit.setVisibility(View.INVISIBLE);

        scoreView = findViewById(R.id.score);
        scoreViewLobby = findViewById(R.id.scoreLobby);
        scoreViewLobby.setVisibility(View.INVISIBLE);

        surfaceView = findViewById(R.id.view);
        surfaceView.getLayoutParams().height = (int) screenPlayHeight;
        surfaceView.setVisibility(View.INVISIBLE);

        eat = MediaPlayer.create(this, R.raw.eat);
        eat.setVolume(audioVolume, audioVolume);

        open = MediaPlayer.create(this, R.raw.open);
        open.setVolume(audioVolume, audioVolume);

        lobbyTrack = MediaPlayer.create(this, R.raw.lobby);
        lobbyTrack.setVolume(musicVolume, musicVolume);
        lobbyTrack.start();

        rl = findViewById(R.id.rl);
        rl.getLayoutParams().height = (int) (screenHeight * 0.1f);
        rl.setVisibility(View.INVISIBLE);

        bottom = findViewById(R.id.bottom);
        bottom.setVisibility(View.INVISIBLE);

        logo = findViewById(R.id.logo);

         detector = new GestureDetector(SnakeActivity.this, new GestureDetector.OnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float v, float v1) {
                // Detect Swipe with direction
                if (isPlaying && !hasChangedDirection) {
                    int changeX = (int) Math.abs(e1.getX() - e2.getX());
                    int changeY = (int) Math.abs(e1.getY() - e2.getY());
                    if (changeX > changeY) {
                        if (e1.getX() > e2.getX()) {
                            if (heading != Heading.RIGHT) heading = Heading.LEFT;
                        } else {
                            if (heading != Heading.LEFT) heading = Heading.RIGHT;
                        }
                    } else {
                        if (e1.getY() > e2.getY()) {
                            if (heading != Heading.DOWN) heading = Heading.UP;
                        } else {
                            if (heading != Heading.UP) heading = Heading.DOWN;
                        }
                    }
                    hasChangedDirection = true;
                    return true;
                }
                return false;
            }


            @Override
            public boolean onDown(MotionEvent motionEvent) {
                return false;
            }

            @Override
            public void onShowPress(MotionEvent motionEvent) {

            }

            @Override
            public boolean onSingleTapUp(MotionEvent motionEvent) {
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
                return false;
            }

            @Override
            public void onLongPress(MotionEvent motionEvent) {

            }
        });
    }

    private void resumeGame() {
        surfaceView.setVisibility(View.VISIBLE);
        for(int i = 0; i < foodTime.length; i++){
            foodTime[i] += System.currentTimeMillis() - prevTime;
        }
        isPlaying = true;
        backTrack.start();
        // Start Thread
        thread = new Thread(this);
        thread.start();
    }

    private void showGameModeDialog() {
        open.start();
        final Dialog gameModeDialog = new Dialog(SnakeActivity.this);
        gameModeDialog.setContentView(R.layout.game_mode_dialog);
        final Button classic = gameModeDialog.findViewById(R.id.classic),
                noWalls = gameModeDialog.findViewById(R.id.no_walls),
                food_x3 = gameModeDialog.findViewById(R.id.foodx3),
                food_x5 = gameModeDialog.findViewById(R.id.foodx5),
                practice = gameModeDialog.findViewById(R.id.practice),
                close = gameModeDialog.findViewById(R.id.closeGameMode);

        classic.setOnClickListener(view -> {
            setGameMode(0);
            gameModeDialog.dismiss();
        });
        noWalls.setOnClickListener(view -> {
            setGameMode(1);
            gameModeDialog.dismiss();
        });
        food_x3.setOnClickListener(view -> {
            setGameMode(2);
            gameModeDialog.dismiss();
        });
        food_x5.setOnClickListener(view -> {
            setGameMode(3);
            gameModeDialog.dismiss();
        });

        practice.setOnClickListener(view -> {
            setGameMode(4);
            gameModeDialog.dismiss();
        });
        close.setOnClickListener(view -> gameModeDialog.dismiss());
        gameModeDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        gameModeDialog.getWindow().setLayout((int) (screenWidth * 0.95f), (int) (screenHeight * 0.75f));
        gameModeDialog.show();
    }

    private void setGameMode(int i) {
        gameMode = i;
        gameModeSelect.setText(gameModeNames[i]);
        if (i == 4) {
            highScoreText.setText("");
        } else {
            highScoreText.setText(String.format(getString(R.string.highscore) + " - %s", highScore[gameMode]));
        }
        scoreViewLobby.setText("");
        SharedPreferences sharedPreferences = getSharedPreferences("values", 0);
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putInt("gamemode", i);
        edit.apply();
    }

    private void showStatsDialog(int i) {
        open.start();
        final Dialog statsDialog = new Dialog(SnakeActivity.this);
        statsDialog.setContentView(R.layout.stats);
        TextView modeText = statsDialog.findViewById(R.id.mode),
                highscoreText = statsDialog.findViewById(R.id.highscore),
                totalScoreText = statsDialog.findViewById(R.id.totalScore),
                gamesPlayedText = statsDialog.findViewById(R.id.gamesPlayed),
                averageText = statsDialog.findViewById(R.id.average);

        if (i == 4) {
            modeText.setText(R.string.practice_stats);
            highscoreText.setText("");
            totalScoreText.setText("");
            gamesPlayedText.setText("");
            averageText.setText("");
            statsDialog.show();
        } else {
            modeText.setText(String.format(getString(R.string.mode) + ": %s", getString(gameModeNames[i])));
            highscoreText.setText(String.format(getString(R.string.highscore) + ": %s", highScore[i]));
            totalScoreText.setText(String.format(getString(R.string.total_score) + ": %s", totalScore[i]));
            gamesPlayedText.setText(String.format(getString(R.string.games_played) + ": %s", gamesPlayed[i]));
            float average = (float) totalScore[i] / gamesPlayed[i];
            if (Float.isNaN(average)) {
                average = 0;
            }
            average = Math.round(average * 10) / 10f;
            averageText.setText(String.format(getString(R.string.average) + ": %s", average));
        }
        Button closeDialog = statsDialog.findViewById(R.id.closeStats);
        closeDialog.setOnClickListener(view -> statsDialog.dismiss());
        statsDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        statsDialog.getWindow().setLayout((int) (screenWidth * 0.95f), (int) (screenHeight * 0.55f));
        statsDialog.show();

    }

    private void setLang(String language) {
        locale = new Locale(language);
        Locale.setDefault(locale);
        Configuration configuration = new Configuration();
        configuration.locale = locale;
        getBaseContext().getResources().updateConfiguration(configuration, getBaseContext().getResources().getDisplayMetrics());
        SharedPreferences sharedPreferences = getSharedPreferences("values", 0);
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putString("lang", language);
        edit.apply();
    }


    private void showShopDialog() {
        open.start();
        final Dialog shopDialog = new Dialog(SnakeActivity.this);
        shopDialog.setContentView(R.layout.shop);

        final LinearLayout red = shopDialog.findViewById(R.id.red);
        red.setOnClickListener(view -> setSelectedColor(red, 0, shopDialog));

        final LinearLayout green = shopDialog.findViewById(R.id.green);
        green.setOnClickListener(view -> setSelectedColor(green, 1, shopDialog));

        final LinearLayout blue = shopDialog.findViewById(R.id.blue);
        blue.setOnClickListener(view -> setSelectedColor(blue, 2, shopDialog));

        final LinearLayout yellow = shopDialog.findViewById(R.id.yellow);
        yellow.setOnClickListener(view -> setSelectedColor(yellow, 3, shopDialog));

        final LinearLayout cyan = shopDialog.findViewById(R.id.cyan);
        cyan.setOnClickListener(view -> setSelectedColor(cyan, 4, shopDialog));

        final LinearLayout magenta = shopDialog.findViewById(R.id.magenta);
        magenta.setOnClickListener(view -> setSelectedColor(magenta, 5, shopDialog));

        final LinearLayout orange = shopDialog.findViewById(R.id.orange);
        orange.setOnClickListener(view -> setSelectedColor(orange, 6, shopDialog));

        final LinearLayout violet = shopDialog.findViewById(R.id.violet);
        violet.setOnClickListener(view -> setSelectedColor(violet, 7, shopDialog));

        final LinearLayout brown = shopDialog.findViewById(R.id.brown);
        brown.setOnClickListener(view -> setSelectedColor(brown, 8, shopDialog));

        final LinearLayout black = shopDialog.findViewById(R.id.black);
        black.setOnClickListener(view -> setSelectedColor(black, 9, shopDialog));

        final LinearLayout gray = shopDialog.findViewById(R.id.gray);
        gray.setOnClickListener(view -> setSelectedColor(gray, 10, shopDialog));

        final LinearLayout white = shopDialog.findViewById(R.id.white);
        white.setOnClickListener(view -> setSelectedColor(white, 11, shopDialog));

        switch (selectedColor) {
            case 0:
                red.setBackgroundResource(R.drawable.popup_back);
                break;
            case 1:
                green.setBackgroundResource(R.drawable.popup_back);
                break;
            case 2:
                blue.setBackgroundResource(R.drawable.popup_back);
                break;
            case 3:
                yellow.setBackgroundResource(R.drawable.popup_back);
                break;
            case 4:
                cyan.setBackgroundResource(R.drawable.popup_back);
                break;
            case 5:
                magenta.setBackgroundResource(R.drawable.popup_back);
                break;
            case 6:
                orange.setBackgroundResource(R.drawable.popup_back);
                break;
            case 7:
                violet.setBackgroundResource(R.drawable.popup_back);
                break;
            case 8:
                brown.setBackgroundResource(R.drawable.popup_back);
                break;
            case 9:
                black.setBackgroundResource(R.drawable.popup_back);
                break;
            case 10:
                gray.setBackgroundResource(R.drawable.popup_back);
                break;
            case 11:
                white.setBackgroundResource(R.drawable.popup_back);
                break;
        }
        Button closeDialog = shopDialog.findViewById(R.id.closeStats);
        closeDialog.setOnClickListener(view -> shopDialog.dismiss());
        shopDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        shopDialog.getWindow().setLayout((int) (screenWidth * 0.95f), (int) (screenHeight * 0.85));
        shopDialog.show();
    }

    private void showSettings() {
        open.start();
        final Dialog settingsDialog = new Dialog(SnakeActivity.this);
        settingsDialog.setContentView(R.layout.settings);

        SeekBar settingsAudio = settingsDialog.findViewById(R.id.audioBar),
                settingsMusic = settingsDialog.findViewById(R.id.musicBar);
        Button closeDialog = settingsDialog.findViewById(R.id.closeDialog),
                changeLang = settingsDialog.findViewById(R.id.changeLang),
                instructions = settingsDialog.findViewById(R.id.instructions);

        closeDialog.setOnClickListener(v -> settingsDialog.dismiss());
        settingsAudio.setProgress((int) (audioVolume * 100));
        settingsMusic.setProgress((int) (musicVolume * 100));
        settingsAudio.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                audioVolume = progress / 100f;
                eat.setVolume(audioVolume, audioVolume);
                open.setVolume(audioVolume, audioVolume);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        settingsMusic.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                musicVolume = progress / 100f;
                lobbyTrack.setVolume(musicVolume, musicVolume);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        settingsDialog.setOnDismissListener(dialogInterface -> {
            SharedPreferences sharedPreferences = getSharedPreferences("values", 0);
            SharedPreferences.Editor edit = sharedPreferences.edit();
            edit.putFloat("musicVol", musicVolume);
            edit.putFloat("audioVol", audioVolume);
            edit.apply();
        });
        changeLang.setOnClickListener(view -> {
            final Dialog changeLangDialog = new Dialog(SnakeActivity.this);
            changeLangDialog.setContentView(R.layout.language);
            Button english = changeLangDialog.findViewById(R.id.english),
                    spanish = changeLangDialog.findViewById(R.id.spanish),
                    russian = changeLangDialog.findViewById(R.id.russian),
                    portuguese = changeLangDialog.findViewById(R.id.portuguese),
                    french = changeLangDialog.findViewById(R.id.french),
                    close = changeLangDialog.findViewById(R.id.closeLang);
            english.setOnClickListener(view1 -> {
                setLang("en");
                recreate();
                changeLangDialog.dismiss();
            });
            spanish.setOnClickListener(view12 -> {
                setLang("es");
                recreate();
                changeLangDialog.dismiss();
            });
            russian.setOnClickListener(view13 -> {
                setLang("ru");
                recreate();
                changeLangDialog.dismiss();
            });
            portuguese.setOnClickListener(view14 -> {
                setLang("pt");
                recreate();
                changeLangDialog.dismiss();
            });
            french.setOnClickListener(view15 -> {
                setLang("fr");
                recreate();
                changeLangDialog.dismiss();
            });
            close.setOnClickListener(view16 -> changeLangDialog.dismiss());
            changeLangDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            changeLangDialog.getWindow().setLayout((int) (screenWidth * 0.95f), (int) (screenHeight * 0.8));
            settingsDialog.dismiss();
            changeLangDialog.show();
        });
        instructions.setOnClickListener(view -> showInstructions());
        settingsDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        settingsDialog.getWindow().setLayout((int) (screenWidth * 0.95f), (int) (screenHeight * 0.65f));
        settingsDialog.show();
    }

    private void showInstructions() {
        final Dialog instructionsDialog = new Dialog(SnakeActivity.this);
        instructionsDialog.setContentView(R.layout.instructions);
        Button close = instructionsDialog.findViewById(R.id.closeDialog);
        close.setOnClickListener(view -> instructionsDialog.dismiss());
        instructionsDialog.show();
        instructionsDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        instructionsDialog.getWindow().setLayout((int) (screenWidth * 0.95f), (int) (screenHeight * 0.9f));
    }

    private void setSelectedColor(LinearLayout l, int i, Dialog dialog) {
        l.setBackgroundResource(R.drawable.popup_back);
        selectedColor = i;
        SharedPreferences sharedPreferences = getSharedPreferences("values", 0);
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putInt("color", i);
        edit.apply();
        dialog.dismiss();
    }

    private void game() {
        // Game Values
        dead = false;
        isPlaying = true;
        paused = false;
        if (gameMode == 3) foodNo = 5;
        else if (gameMode == 2) foodNo = 3;
        else foodNo = 1;
        snakeX = new int[374];
        snakeY = new int[374];
        foodX = new int[foodNo];
        foodY = new int[foodNo];
        foodTime = new long[foodNo];
        foodVisible = new boolean[foodNo];
        foodBitmap = new Bitmap[foodNo];
        scoreFood = new int[foodNo];
        snakeX[0] = SCREEN_BLOCKS_WIDTH / 2;
        snakeY[0] = SCREEN_BLOCKS_HEIGHT / 2;
        heading = Heading.STILL;
        snakeLength = 1;
        score = 0;
        scoreMilstone = 0;
        FPS = 6;
        // Visibility
        start.setVisibility(View.INVISIBLE);
        scoreViewLobby.setVisibility(View.INVISIBLE);
        rl.setVisibility(View.VISIBLE);
        surfaceView.setVisibility(View.VISIBLE);
        bottom.setVisibility(View.VISIBLE);
        pause.setBackgroundResource(R.drawable.pause);
        highScoreText.setVisibility(View.INVISIBLE);
        gameModeSelect.setVisibility(View.INVISIBLE);
        quit.setVisibility(View.VISIBLE);
        stats.setVisibility(View.INVISIBLE);
        star.setVisibility(View.INVISIBLE);
        settings.setVisibility(View.INVISIBLE);
        shop.setVisibility(View.INVISIBLE);
        logo.setVisibility(View.INVISIBLE);

        lobbyTrack.stop();

        backTrack = MediaPlayer.create(this, R.raw.backmusic);
        backTrack.setVolume(musicVolume, musicVolume);
        backTrack.setLooping(true);
        backTrack.start();

        // Functions and Thread
        spawnFood(-1);
        thread = new Thread(this);
        thread.start();
        nextFrameTime = System.currentTimeMillis();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        if (lobbyTrack != null && !isPlaying) lobbyTrack.start();
    }

    private void spawnFood(int no) {
        if (no == -1) { // Spawn at Start
            for (int i = 0; i < foodNo; i++) {
                Random random = new Random();
                int x, y;
                boolean inIllegalPosition;
                // To make sure food is not in another food, snake or obstacle
                do {
                    inIllegalPosition = false;
                    x = random.nextInt(SCREEN_BLOCKS_WIDTH);
                    y = random.nextInt(SCREEN_BLOCKS_HEIGHT);
                    // Check if in Snake
                    for (int j = 0; j < snakeLength; j++) {
                        if (snakeX[j] == x && snakeY[j] == y) {
                            inIllegalPosition = true;
                            break;

                        }
                    }
                    // Check if in Food
                    for (int j = 0; j < foodX.length; j++) {
                        if (foodX[j] == x && foodY[j] == y) {
                            inIllegalPosition = true;
                            break;
                        }
                    }
                } while (inIllegalPosition);
                foodX[i] = x;
                foodY[i] = y;
                foodBitmap[i] = getApple(i);
                foodVisible[i] = true;
            }
        } else { // Spawn for specific number
            Random random = new Random();
            int x, y;
            boolean inIllegalPosition;
            do {// To make sure food is not in another food, snake or obstacle
                inIllegalPosition = false;
                x = random.nextInt(SCREEN_BLOCKS_WIDTH);
                y = random.nextInt(SCREEN_BLOCKS_HEIGHT);
                // Check if in Snake
                for (int j = 0; j < snakeLength; j++) {
                    if (snakeX[j] == x && snakeY[j] == y) {
                        inIllegalPosition = true;
                        break;

                    }
                }
                // Check if in Food
                for (int j = 0; j < foodX.length; j++) {
                    if (foodX[j] == x && foodY[j] == y) {
                        inIllegalPosition = true;
                        break;
                    }
                }
            } while (inIllegalPosition);
            foodX[no] = x;
            foodY[no] = y;
            foodBitmap[no] = getApple(no);
            foodVisible[no] = true;
        }
    }

    @Override
    public void run() {
        while (isPlaying) {
            if (updateRequired()) {
                if (paused) pause();
                else if (update() || dead) {
                    // Go to Lobby
                    runOnUiThread(() -> {
                        Handler handler = new Handler();
                        handler.postDelayed(() -> lobby(gameMode), 2000);
                    });
                    backTrack.stop();
                    // Vibrate Phone
                    Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        v.vibrate(500);
                    }

                    // Stop Thread
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                } else {
                    draw();
                    // Update Score Text
                    runOnUiThread(() -> scoreView.setText(String.format("%s", score)));
                }
            }
        }
    }

    private boolean updateRequired() {
        if (nextFrameTime <= System.currentTimeMillis()) {
            nextFrameTime = System.currentTimeMillis() + 1000 / FPS;
            return true;
        }
        return false;
    }

    private void pause() {
        runOnUiThread(() -> surfaceView.setVisibility(View.INVISIBLE));
        isPlaying = false;
        backTrack.pause();
        prevTime = System.currentTimeMillis();
        // Stop Thread
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean update() {
        for (int i = 0; i < foodNo; i++) {
            // Check if food is eaten
            if (snakeX[0] == foodX[i] && snakeY[0] == foodY[i]) {
                eatFood(i);
                Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    v.vibrate(100);
                }

            }
            // Check if food's time is up
            if (System.currentTimeMillis() >= foodTime[i]) {
                spawnFood(i);
            }
            // Food time should not decrease when game not started
            if (heading == Heading.STILL) {
                if (foodBitmap[i] == apple) foodTime[i] = System.currentTimeMillis() + appleTime;
                else if (foodBitmap[i] == sapple)
                    foodTime[i] = System.currentTimeMillis() + sappleTime;
                else if (foodBitmap[i] == gapple)
                    foodTime[i] = System.currentTimeMillis() + gappleTime;
                else if (foodBitmap[i] == godapple)
                    foodTime[i] = System.currentTimeMillis() + godappleTime;
            }
            hasChangedDirection = false;
        }
        moveSnake();
        return detectDeath();
    }

    private void eatFood(int no) {
        snakeLength++;
        score += scoreFood[no];
        if (score - scoreMilstone >= 150) {
            FPS++;
            scoreMilstone += 150;
        }
        // To show added score text near eaten food (+10, +20, +30, +50)
        if (foodX[no] > SCREEN_BLOCKS_WIDTH / 2) scoreAddX = foodX[no] - 1;
        else scoreAddX = foodX[no] + 1;
        if (foodY[no] > SCREEN_BLOCKS_HEIGHT / 2) scoreAddY = foodY[no] - 1;
        else scoreAddY = foodY[no] + 1;
        scoreAdd = scoreFood[no];
        scoreAddTime = System.currentTimeMillis() + 2000;
        alpha = 255;
        eat.stop();
        eat = MediaPlayer.create(this, R.raw.eat);
        eat.setVolume(audioVolume, audioVolume);
        eat.start();
        // Spawn Another Food
        spawnFood(no);
    }

    private Bitmap getApple(int no) {
        Random random = new Random();
        int i = random.nextInt(100);
        if (i < 65) {
            // Spawn Apple
            foodTime[no] = System.currentTimeMillis() + (int) (appleTime * 8.0 / FPS);
            scoreFood[no] = 10;
            return apple;
        } else if (i < 85) {
            // Spawn Sapple
            foodTime[no] = System.currentTimeMillis() + (int) (sappleTime * 8.0 / FPS);
            scoreFood[no] = 20;
            return sapple;
        } else if (i < 95) {
            // Spawn Gapple
            foodTime[no] = System.currentTimeMillis() + (int) (gappleTime * 8.0 / FPS);
            scoreFood[no] = 30;
            return gapple;
        } else {
            // Spawn GodApple
            foodTime[no] = System.currentTimeMillis() + (int) (godappleTime * 8.0 / FPS);
            scoreFood[no] = 50;
            return godapple;
        }
    }

    private void moveSnake() {
        if (heading != Heading.STILL) {
            // Move Body
            for (int i = snakeLength; i > 0; i--) {
                snakeX[i] = snakeX[i - 1];
                snakeY[i] = snakeY[i - 1];
            }
            // Move Head
            switch (heading) {
                case UP:
                    snakeY[0] -= 1;
                    break;
                case DOWN:
                    snakeY[0] += 1;
                    break;
                case RIGHT:
                    snakeX[0] += 1;
                    break;
                case LEFT:
                    snakeX[0] -= 1;
                    break;
            }
            // Warp
            if (gameMode == 1 || gameMode == 4) {
                if (snakeX[0] < 0) snakeX[0] = SCREEN_BLOCKS_WIDTH - 1;
                else if (snakeX[0] == SCREEN_BLOCKS_WIDTH) snakeX[0] = 0;
                if (snakeY[0] < 0) snakeY[0] = SCREEN_BLOCKS_HEIGHT - 1;
                else if (snakeY[0] == SCREEN_BLOCKS_HEIGHT) snakeY[0] = 0;
            }
        }
    }

    private boolean detectDeath() {
        boolean dead = false;
        if (gameMode != 4) {
            if (gameMode != 1) {
                // Check if snake hit edge
                if (snakeX[0] == -1) dead = true;
                else if (snakeX[0] >= SCREEN_BLOCKS_WIDTH) dead = true;
                else if (snakeY[0] == -1) dead = true;
                else if (snakeY[0] >= SCREEN_BLOCKS_HEIGHT) dead = true;
            }
            // Check if snake hit itself
            for (int i = snakeLength - 1; i > 0; i--) {
                if (snakeX[0] == snakeX[i] && snakeY[0] == snakeY[i]) {
                    dead = true;
                    break;
                }
            }
        }
        return dead;
    }

    private void lobby(int i) {
        // Visibility
        surfaceView.setVisibility(View.INVISIBLE);
        quit.setVisibility(View.INVISIBLE);
        rl.setVisibility(View.INVISIBLE);
        bottom.setVisibility(View.INVISIBLE);
        start.setVisibility(View.VISIBLE);
        scoreViewLobby.setVisibility(View.VISIBLE);
        highScoreText.setVisibility(View.VISIBLE);
        gameModeSelect.setVisibility(View.VISIBLE);
        stats.setVisibility(View.VISIBLE);
        star.setVisibility(View.VISIBLE);
        settings.setVisibility(View.VISIBLE);
        shop.setVisibility(View.VISIBLE);
        logo.setVisibility(View.VISIBLE);

        setLang(locale.toLanguageTag());
        // Values
        isPlaying = false;
        if (i == 4) {
            scoreViewLobby.setText("");
            return;
        }
        if (score > highScore[i]) {
            highScore[i] = score;
            highScoreText.setText(R.string.newHighScore);
        }
        lobbyTrack = MediaPlayer.create(this, R.raw.lobby);
        lobbyTrack.setVolume(musicVolume, musicVolume);
        lobbyTrack.start();
        scoreViewLobby.setText(String.format("%s", score));
        gamesPlayed[i] += 1;
        totalScore[i] += score;
        SharedPreferences sharedPreferences = getSharedPreferences("values", 0);
        SharedPreferences.Editor edit = sharedPreferences.edit();
        Gson gson = new Gson();
        String hs = gson.toJson(highScore);
        String ts = gson.toJson(totalScore);
        String gp = gson.toJson(gamesPlayed);
        edit.putString("hs", hs);
        edit.putString("ts", ts);
        edit.putString("gp", gp);
        edit.apply();
    }


    private void draw() {
        if (surfaceView.getHolder().getSurface().isValid()) {
            Canvas canvas = surfaceView.getHolder().lockCanvas();

            // Draw Background
            canvas.drawColor(/*Color.BLACK*/getResources().getColor(R.color.colorPrimary));

            // Draw Food
            for (int i = 0; i < foodNo; i++) {
                if (foodTime[i] - System.currentTimeMillis() <= 3000) {
                    // Blinking food
                    if (foodVisible[i]) {
                        foodVisible[i] = false;
                    } else {
                        canvas.drawBitmap(foodBitmap[i], foodX[i] * blockWidth, foodY[i] * blockHeight, null);
                        foodVisible[i] = true;
                    }
                } else
                    canvas.drawBitmap(foodBitmap[i], foodX[i] * blockWidth, foodY[i] * blockHeight, null);
            }
            int red = skins[selectedColor][0], green = skins[selectedColor][1], blue = skins[selectedColor][2];
            // Draw Snake
            paint.setColor(Color.rgb(red, green, blue));
            for (int i = 0; i < snakeLength; i++) {
                canvas.drawRect(snakeX[i] * blockWidth, (float) snakeY[i] * blockHeight, snakeX[i] * blockWidth + blockWidth, snakeY[i] * blockHeight + blockHeight, paint);
            }
            // Draw Added Score (+10, +20, +30, +50)
            if (scoreAddTime > System.currentTimeMillis()) {
                if (selectedColor == 1 || selectedColor == 3 || selectedColor == 4 || selectedColor == 5 || selectedColor == 11) {
                    paint.setColor(Color.BLACK);
                } else {
                    paint.setColor(Color.WHITE);
                }
                paint.setAlpha(alpha);
                alpha = alpha - 15;
                canvas.drawText(String.format("+%S", scoreAdd), scoreAddX * blockWidth, scoreAddY * blockHeight, paint);
            }
            surfaceView.getHolder().unlockCanvasAndPost(canvas);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        lobbyTrack.pause();
        paused = true;
        pause.setBackgroundResource(R.drawable.play);
    }



    @Override
    public boolean onTouchEvent(MotionEvent event) {
        detector.onTouchEvent(event);
        return super.onTouchEvent(event);
    }


}