package com.example.songapp;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.songapp.databinding.FragmentFirstBinding;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private int mNumPlayers;
    private Bundle mBundle;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        ((SelectPlaylist) getActivity()).setActionBarTitle("Select Playlist");
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.btn90s.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBundle = new Bundle();
                mBundle.putString("playlistUri", Constants.PLAYLIST_NINETIES);

                showNumPlayersDialog();
//                NavHostFragment.findNavController(FirstFragment.this)
//                        .navigate(R.id.action_FirstFragment_to_SecondFragment, bundle);
            }
        });

        binding.btn00s.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBundle = new Bundle();
                mBundle.putString("playlistUri", Constants.PLAYLIST_TWO_THOUSANDS);

                showNumPlayersDialog();
//                NavHostFragment.findNavController(FirstFragment.this)
//                        .navigate(R.id.action_FirstFragment_to_SecondFragment, bundle);
            }
        });

        binding.btn10s.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBundle = new Bundle();
                mBundle.putString("playlistUri", Constants.PLAYLIST_TWENTY_TENS);

                showNumPlayersDialog();
//                NavHostFragment.findNavController(FirstFragment.this)
//                        .navigate(R.id.action_FirstFragment_to_SecondFragment, bundle);
            }
        });

        binding.btnCustom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBundle = new Bundle();
                showCustomPlaylistDialog();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void showCustomPlaylistDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Paste the link to the playlist.");
        builder.setMessage("Open a Spotify playlist > Three dots > Share > Copy link to playlist");

        final EditText link = new EditText(getContext());

        link.setInputType(InputType.TYPE_CLASS_TEXT);
        link.setHint("https://open.spotify.com/playlist/...");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(link);

        builder.setView(layout);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String playlistUri = Utils.parsePlaylistLink(link.getText().toString());

                if(!playlistUri.isEmpty())
                {
                    String fullPlaylistUri = "spotify:playlist:" + playlistUri;
                    mBundle.putString("playlistUri", fullPlaylistUri);

//                    NavHostFragment.findNavController(FirstFragment.this)
//                            .navigate(R.id.action_FirstFragment_to_SecondFragment, bundle);
                    showNumPlayersDialog();
                }
                else
                {
                    Toast invalidLink =
                            Toast.makeText(getContext(), "Invalid playlist link", Toast.LENGTH_SHORT);
                    invalidLink.show();
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        builder.show();
    }

    private void showNumPlayersDialog()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("How many players?");

        final EditText numPlayersText = new EditText(getContext());

        numPlayersText.setInputType(InputType.TYPE_CLASS_NUMBER);
        numPlayersText.setHint("Enter a number between 1-4");

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(numPlayersText);

        builder.setView(layout);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                int numPlayers;

                if(numPlayersText.getText().toString().isEmpty())
                {
                    Toast invalidLink =
                            Toast.makeText(getContext(), "Invalid number", Toast.LENGTH_SHORT);
                    invalidLink.show();
                    return;
                }

                numPlayers = Integer.parseInt(numPlayersText.getText().toString());

                if(numPlayers > 0 && numPlayers < 5)
                {
                    mNumPlayers = numPlayers;

                    mBundle.putInt("numPlayers", mNumPlayers);

                    NavHostFragment.findNavController(FirstFragment.this)
                            .navigate(R.id.action_FirstFragment_to_SecondFragment, mBundle);
                }
                else
                {
                    Toast invalidLink =
                            Toast.makeText(getContext(), "Invalid number", Toast.LENGTH_SHORT);
                    invalidLink.show();
                }
            }
        });

        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        builder.show();
    }
}