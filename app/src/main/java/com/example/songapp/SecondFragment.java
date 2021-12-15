package com.example.songapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.InputType;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.songapp.databinding.FragmentSecondBinding;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SecondFragment extends Fragment {

    private static final String CLIENT_ID = "0429b0ef724f4133932495f0be8f60dd";
    private static final String REDIRECT_URI = "http://localhost:8080/";

    private static final long PLAY_CLIP_DURATION_MS = 15000;
    private static final long DEFAULT_STARTING_POSITION_MS = 60000;

    private SpotifyAppRemote mSpotifyAppRemote;
    private Subscription<PlayerState> mPlayerStateSubscription;

    private SpeechRecognizer mSpeechRecognizer;
    private Pair<Boolean, Boolean> mSpeechValidationResults = new Pair<>(false, false);

    private String mCurrentName;
    private String mCurrentArtist;
    private String guessedName;
    private String guessedArtist;
    private String currentPlaylistUri;

    private int mCurrentPlayer = 1;
    private int mNumPlayers = 1;
    private Map<String, Pair<TextView, TextView>> mPlayerScoreMap = new HashMap<>();

    private int mCurrentRound = 1;
    private int mTotalRounds = Constants.DEFAULT_NUM_ROUNDS;

    private long mStartingPoint = -1;

    private FragmentSecondBinding binding;

    private final Subscription.EventCallback<PlayerState> mPlayerStateEventCallback =
            new Subscription.EventCallback<PlayerState>() {
                @Override
                public void onEvent(PlayerState playerState) {
                    final Track track = playerState.track;

                    if(track != null)
                    {
                        mStartingPoint = track.duration / 3;
                        mCurrentName = track.name;
                        mCurrentArtist = track.artist.name;
                    }
                }
            };

    private final RecognitionListener mRecognitionListener =
            new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle bundle) {

                }

                @Override
                public void onBeginningOfSpeech() {

                }

                @Override
                public void onRmsChanged(float v) {

                }

                @Override
                public void onBufferReceived(byte[] bytes) {

                }

                @Override
                public void onEndOfSpeech() {

                }

                @Override
                public void onError(int i) {

                }

                @Override
                public void onResults(Bundle bundle) {
                    ArrayList<String> results = (ArrayList<String>) bundle.get(SpeechRecognizer.RESULTS_RECOGNITION);

                    if(!results.isEmpty())
                    {
                        Log.d("SecondFragment", "******Calling parseGuess from onResult in listener");
                        String guess = results.get(0);
                        mSpeechValidationResults = Utils.parseGuess(guess, mCurrentName, mCurrentArtist);
                    }

                    showResult(mSpeechValidationResults);
                }

                @Override
                public void onPartialResults(Bundle bundle) {

                }

                @Override
                public void onEvent(int i, Bundle bundle) {

                }
            };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        binding = FragmentSecondBinding.inflate(inflater, container, false);
        ((SelectPlaylist) getActivity()).setActionBarTitle("Guess That Song");
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(getContext());
        mSpeechRecognizer.setRecognitionListener(mRecognitionListener);

        mNumPlayers = getArguments().getInt("numPlayers");
        setPlayerTextPositions(mNumPlayers);

        binding.btnNextSong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(mCurrentRound == mTotalRounds && mCurrentPlayer == mNumPlayers)
                {
                    NavHostFragment.findNavController(SecondFragment.this)
                            .navigate(R.id.action_SecondFragment_to_FirstFragment);
                    return;
                }

                if(mCurrentPlayer == mNumPlayers)
                {
                    mCurrentRound++;
                    TextView textCurrentRound = getView().findViewById(R.id.textCurrentRound);
                    textCurrentRound.setText(Integer.toString(mCurrentRound));
                }

                moveToNextPlayer();

                mSpotifyAppRemote.getPlayerApi().skipNext();
                startGuess();
            }
        });

        binding.btnNextSong.setVisibility(View.INVISIBLE);

//        binding.btnDone.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                mSpeechRecognizer.stopListening();
//                binding.btnDone.setVisibility(View.INVISIBLE);
//            }
//        });
//
//        binding.btnDone.setVisibility(View.INVISIBLE);

        // Set the connection parameters
        ConnectionParams connectionParams =
                new ConnectionParams.Builder(CLIENT_ID)
                        .setRedirectUri(REDIRECT_URI)
                        .showAuthView(true)
                        .build();

        SpotifyAppRemote.connect(getContext(), connectionParams,
                new Connector.ConnectionListener() {

                    @Override
                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                        mSpotifyAppRemote = spotifyAppRemote;
                        Log.d("MainActivity", "Connected! Yay!");

                        mSpotifyAppRemote.getPlayerApi()
                                .subscribeToPlayerState()
                                .setEventCallback(mPlayerStateEventCallback);
                        connected();
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        Log.e("MainActivity", throwable.getMessage(), throwable);

                        // Something went wrong when attempting to connect! Handle errors here
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;

        SpotifyAppRemote.disconnect(mSpotifyAppRemote);
    }

    private void connected()
    {
        mSpotifyAppRemote.getPlayerApi().setShuffle(true);

        String playlistUri = getArguments().getString("playlistUri");
        currentPlaylistUri = playlistUri;

        mSpotifyAppRemote.getPlayerApi().play(playlistUri);

        // can't figure out why the player won't allow a seekTo
        // immediately after starting a new playlist for the first time.
        // this is a workaround to give some time to startup before
        // starting the song at the default starting position.
        try {
            Thread.sleep(100);
        }
        catch(InterruptedException e)
        {
        }

        startGuess();
    }

    private void startGuess()
    {
        binding.btnNextSong.setVisibility(View.INVISIBLE);

        if(mStartingPoint == -1)
        {
            mStartingPoint = DEFAULT_STARTING_POSITION_MS;
        }

        mSpotifyAppRemote.getPlayerApi().seekTo(mStartingPoint);

        mSpotifyAppRemote.getPlayerApi().getPlayerState()
                .setResultCallback(playerState -> {
                    if(playerState.isPaused)
                    {
                        mSpotifyAppRemote.getPlayerApi().resume();
                    }
                });

        TextView status = getView().findViewById(R.id.status);
        TextView name = getView().findViewById(R.id.name);
        TextView artist = getView().findViewById(R.id.artist);

        name.setText("");
        artist.setText("");
        status.setText("Playing clip...");

        ImageView soundWaves = getView().findViewById(R.id.sound_waves2);
        soundWaves.setVisibility(View.VISIBLE);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mSpotifyAppRemote.getPlayerApi().pause();
                // showGuessDialog();
                status.setText("Listening...");
                soundWaves.setVisibility(View.INVISIBLE);

                Intent speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

                if(getActivity() != null && isAdded())
                {
                    startActivityForResult(speechIntent, 0);
                }

                mSpeechRecognizer.startListening(speechIntent);

                // binding.btnDone.setVisibility(View.VISIBLE);
            }
        }, PLAY_CLIP_DURATION_MS);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        String guess = "";

        if(requestCode == 0 && data != null)
        {
            ArrayList<String> results = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            guess = results.get(0);

            if(guess.equalsIgnoreCase("repeat") ||
            guess.equalsIgnoreCase("replay"))
            {
                startGuess();
                return;
            }
        }

        Log.d("SecondFragment:onActivityResult", "resultCode: " + resultCode);
        mSpeechValidationResults = Utils.parseGuess(guess, mCurrentName, mCurrentArtist);

        showResult(mSpeechValidationResults);
    }

    private void showGuessDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Guess the song name & artist!");
        builder.setCancelable(false);

        final EditText name = new EditText(getContext());
        final EditText artist = new EditText(getContext());

        name.setInputType(InputType.TYPE_CLASS_TEXT);
        name.setHint("Name");
        artist.setInputType(InputType.TYPE_CLASS_TEXT);
        artist.setHint("Artist");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(name);
        layout.addView(artist);

        builder.setView(layout);

        builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                guessedName = name.getText().toString();
                guessedArtist = artist.getText().toString();

                // validateGuess(guessedName, guessedArtist);
            }
        });

        builder.setNegativeButton("Pass", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
                guessedName = "";
                guessedArtist = "";
                // validateGuess(guessedName, guessedArtist);
            }
        });

        builder.setNeutralButton("Repeat Clip", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
                startGuess();
            }
        });

        builder.show();
    }

//    private void validateGuess(String guessedName, String guessedArtist)
//    {
//        boolean isNameCorrect = false;
//        boolean isArtistCorrect = false;
//
//        TextView name = getView().findViewById(R.id.name);
//        TextView artist = getView().findViewById(R.id.artist);
//        TextView status = getView().findViewById(R.id.status);
//
//        name.setText(currentName);
//        artist.setText(currentArtist);
//        name.setTextColor(Color.WHITE);
//        artist.setTextColor(Color.WHITE);
//
//        if(!guessedName.isEmpty() && (guessedName.equalsIgnoreCase(currentName) ||
//            currentName.toLowerCase().contains(guessedName.toLowerCase())))
//        {
//            name.setTextColor(Color.GREEN);
//            isNameCorrect = true;
//        }
//        if(!guessedArtist.isEmpty() && (guessedArtist.equalsIgnoreCase(currentArtist) ||
//            currentArtist.toLowerCase().contains(guessedArtist.toLowerCase())))
//        {
//            artist.setTextColor(Color.GREEN);
//            isArtistCorrect = true;
//        }
//
//        if(isNameCorrect && isArtistCorrect)
//        {
//            status.setText("You got the name & artist!");
//        }
//        else if(isNameCorrect)
//        {
//            status.setText("You got the name!");
//        }
//        else if(isArtistCorrect)
//        {
//            status.setText("You got the artist!");
//        }
//        else
//        {
//            status.setText("That was trash.");
//        }
//
//        binding.btnNextSong.setVisibility(View.VISIBLE);
//    }

    private void showResult(Pair<Boolean, Boolean> nameArtistResult)
    {
        MediaPlayer mp;
        int earnedPoints = 0;

        TextView name = getView().findViewById(R.id.name);
        TextView artist = getView().findViewById(R.id.artist);
        TextView status = getView().findViewById(R.id.status);

        name.setText(mCurrentName);
        artist.setText(mCurrentArtist);
        name.setTextColor(Color.WHITE);
        artist.setTextColor(Color.WHITE);

        if(nameArtistResult.first && nameArtistResult.second)
        {
            name.setTextColor(Color.CYAN);
            artist.setTextColor(Color.CYAN);
            status.setText("You got the name & artist!");
            earnedPoints = 20;
            mp = MediaPlayer.create(getContext(), R.raw.smb_1up);
        }
        else if(nameArtistResult.first)
        {
            name.setTextColor(Color.CYAN);
            status.setText("You got the name!");
            earnedPoints = 10;
            mp = MediaPlayer.create(getContext(), R.raw.smb_coin);
        }
        else if(nameArtistResult.second)
        {
            artist.setTextColor(Color.CYAN);
            status.setText("You got the artist!");
            earnedPoints = 10;
            mp = MediaPlayer.create(getContext(), R.raw.smb_coin);
        }
        else
        {
            status.setText("That was trash.");

            mp = MediaPlayer.create(getContext(), R.raw.minecraft_oof);
        }

        assignPoints(earnedPoints);
        mp.start();

        Log.d("SecondFragment", "mCurrentRound: " + mCurrentRound);
        Log.d("SecondFragment", "mTotalRounds: " + mTotalRounds);
        Log.d("SecondFragment", "mCurrentPlayer: " + mCurrentPlayer);
        Log.d("SecondFragment", "mNumPlayers: " + mNumPlayers);

        if(mCurrentRound == mTotalRounds && mCurrentPlayer == mNumPlayers)
        {
            // game over
            String winner = determineWinner();
            status.setText(winner + " Wins!");
            binding.btnNextSong.setText("Play Again");
        }

        binding.btnNextSong.setVisibility(View.VISIBLE);
    }

    private void setPlayerTextPositions(int numPlayers)
    {
        TextView player1Name = getView().findViewById(R.id.player1Name);
        TextView player1Score = getView().findViewById(R.id.player1Score);
        TextView player2Name = getView().findViewById(R.id.player2Name);
        TextView player2Score = getView().findViewById(R.id.player2Score);
        TextView player3Name = getView().findViewById(R.id.player3Name);
        TextView player3Score = getView().findViewById(R.id.player3Score);
        TextView player4Name = getView().findViewById(R.id.player4Name);
        TextView player4Score = getView().findViewById(R.id.player4Score);

        player1Name.setTextColor(Color.CYAN);
        player1Score.setTextColor(Color.CYAN);

        mPlayerScoreMap.put("player1", new Pair<>(player1Name, player1Score));
        mPlayerScoreMap.put("player2", new Pair<>(player2Name, player2Score));
        mPlayerScoreMap.put("player3", new Pair<>(player3Name, player3Score));
        mPlayerScoreMap.put("player4", new Pair<>(player4Name, player4Score));

        switch (numPlayers)
        {
            case 1:
                player2Name.setVisibility(View.INVISIBLE);
                player2Score.setVisibility(View.INVISIBLE);
                player3Name.setVisibility(View.INVISIBLE);
                player3Score.setVisibility(View.INVISIBLE);
                player4Name.setVisibility(View.INVISIBLE);
                player4Score.setVisibility(View.INVISIBLE);
                break;
            case 2:
                player1Name.setTranslationX(-150.0f);
                player1Score.setTranslationX(-150.0f);
                player2Name.setTranslationX(150.0f);
                player2Score.setTranslationX(150.0f);
                player3Name.setVisibility(View.INVISIBLE);
                player3Score.setVisibility(View.INVISIBLE);
                player4Name.setVisibility(View.INVISIBLE);
                player4Score.setVisibility(View.INVISIBLE);
                break;
            case 3:
                player1Name.setTranslationX(-250.0f);
                player1Score.setTranslationX(-250.0f);
                player3Name.setTranslationX(250.0f);
                player3Score.setTranslationX(250.0f);
                player4Name.setVisibility(View.INVISIBLE);
                player4Score.setVisibility(View.INVISIBLE);
                break;
            case 4:
                player1Name.setTranslationX(-400.0f);
                player1Score.setTranslationX(-400.0f);
                player2Name.setTranslationX(-200.0f);
                player2Score.setTranslationX(-200.0f);
                player3Name.setTranslationX(200.0f);
                player3Score.setTranslationX(200.0f);
                player4Name.setTranslationX(400.0f);
                player4Score.setTranslationX(400.0f);
                break;
            default:
                break;
        }
    }

    private void assignPoints(int earnedPoints)
    {
        String currentPlayer = "player" + mCurrentPlayer;

        Pair<TextView, TextView> playerScore = mPlayerScoreMap.get(currentPlayer);
        int currentPoints = Integer.parseInt(playerScore.second.getText().toString());

        currentPoints += earnedPoints;

        playerScore.second.setText(Integer.toString(currentPoints));
    }

    private void moveToNextPlayer()
    {
        if(mCurrentPlayer == mNumPlayers)
        {
            mCurrentPlayer = 1;
        }
        else
        {
            mCurrentPlayer++;
        }

        for(Pair<TextView, TextView> playerScore : mPlayerScoreMap.values())
        {
            playerScore.first.setTextColor(Color.WHITE);
            playerScore.second.setTextColor(Color.WHITE);
        }

        String currentPlayer = "player" + mCurrentPlayer;
        Pair<TextView, TextView> currentPlayerScore = mPlayerScoreMap.get(currentPlayer);
        currentPlayerScore.first.setTextColor(Color.CYAN);
        currentPlayerScore.second.setTextColor(Color.CYAN);
    }

    private String determineWinner()
    {
        int max = 0;
        String winner = "P1";
        Map<String, Integer> finalScores = new HashMap<>();

        for(Pair<TextView, TextView> playerScore : mPlayerScoreMap.values())
        {
            finalScores.put(playerScore.first.getText().toString(),
                    Integer.parseInt(playerScore.second.getText().toString()));
        }

        for(Map.Entry<String, Integer> entry : finalScores.entrySet())
        {
            if(entry.getValue() > max)
            {
                max = entry.getValue();
                winner = entry.getKey();
            }
            else if(entry.getValue() == max)
            {
                winner += entry.getKey();
            }
        }

        return winner;
    }
}