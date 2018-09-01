/*
 *  /**
 *  * Copyright (C) 2017  Grbl Controller Contributors
 *  *
 *  * This program is free software; you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation; either version 2 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  * with this program; if not, write to the Free Software Foundation, Inc.,
 *  * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *  * <http://www.gnu.org/licenses/>
 *
 */

package in.co.gorest.grblcontroller.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.crashlytics.android.answers.Answers;
import com.crashlytics.android.answers.CustomEvent;
import com.joanzapata.iconify.widget.IconButton;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Objects;

import in.co.gorest.grblcontroller.R;
import in.co.gorest.grblcontroller.databinding.FragmentProbingTabBinding;
import in.co.gorest.grblcontroller.events.GrblProbeEvent;
import in.co.gorest.grblcontroller.events.UiToastEvent;
import in.co.gorest.grblcontroller.helpers.EnhancedSharedPreferences;
import in.co.gorest.grblcontroller.listeners.MachineStatusListener;
import in.co.gorest.grblcontroller.model.Constants;
import in.co.gorest.grblcontroller.util.GrblUtils;

public class ProbingTabFragment extends BaseFragment {

    private static final String TAG = ProbingTabFragment.class.getSimpleName();
    private MachineStatusListener machineStatus;
    private EnhancedSharedPreferences sharedPref;

    private TextView probingFeedRate, probingPlateThickness, probingDistance;
    private Double probeStartPosition = null;
    private SwitchCompat autoZeroAfterProbe;
    private Integer probeType;

    public ProbingTabFragment() {}

    public static ProbingTabFragment newInstance() {
        return new ProbingTabFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        machineStatus = MachineStatusListener.getInstance();
        sharedPref = EnhancedSharedPreferences.getInstance(Objects.requireNonNull(getActivity()).getApplicationContext(), getString(R.string.shared_preference_key));
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        FragmentProbingTabBinding binding = DataBindingUtil.inflate(inflater, R.layout.fragment_probing_tab, container, false);
        binding.setMachineStatus(machineStatus);
        View view = binding.getRoot();

        final RelativeLayout probingFeedRateView = view.findViewById(R.id.probing_feed_rate_view);
        probingFeedRateView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setProbingFeedRate();
            }
        });

        RelativeLayout probingPlateThicknessView = view.findViewById(R.id.probing_plate_thickness_view);
        probingPlateThicknessView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setProbingPlateThickness();
            }
        });

        final RelativeLayout probingDistanceView = view.findViewById(R.id.probing_distance_view);
        probingDistanceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setProbingDistance();
            }
        });

        probingFeedRate = view.findViewById(R.id.probing_feed_rate);
        probingFeedRate.setText(sharedPref.getString(getString(R.string.preference_probing_feed_rate), String.valueOf(Constants.PROBING_FEED_RATE)));

        probingPlateThickness = view.findViewById(R.id.probing_plate_thickness);
        probingPlateThickness.setText(sharedPref.getString(getString(R.string.preference_probing_plate_thickness), String.valueOf(Constants.PROBING_PLATE_THICKNESS)));

        probingDistance = view.findViewById(R.id.probing_distance);
        probingDistance.setText(sharedPref.getString(getString(R.string.preference_probing_distance), String.valueOf(Constants.PROBING_DISTANCE)));

        IconButton startProbe = view.findViewById(R.id.start_probe);
        startProbe.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.text_straight_probe))
                        .setMessage(getString(R.string.text_straight_probe_desc))
                        .setPositiveButton(getString(R.string.text_yes_confirm), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                probeType = Constants.PROBE_TYPE_NORMAL;
                                doProbing();
                            }
                        })
                        .setNegativeButton(getString(R.string.text_cancel), null)
                        .show();

            }
        });

        IconButton startToolOffset = view.findViewById(R.id.start_tool_length_offset);
        startToolOffset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(machineStatus.getLastProbePosition() == null){
                    EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_last_probe_location_unknown)));
                    return;
                }

                new AlertDialog.Builder(getActivity())
                        .setTitle(getString(R.string.text_dynamic_tool_length_offset))
                        .setMessage(getString(R.string.text_dynamic_tlo_desc))
                        .setPositiveButton(getString(R.string.text_ok), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                probeType = Constants.PROBE_TYPE_TOOL_OFFSET;
                                doProbing();
                            }
                        })
                        .setNegativeButton(getString(R.string.text_cancel), null)
                        .show();
            }
        });

        IconButton cancelToolOffset = view.findViewById(R.id.cancel_tool_offset);
        cancelToolOffset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(machineStatus.getState().equals(Constants.MACHINE_STATUS_IDLE)){
                    new AlertDialog.Builder(getActivity())
                            .setTitle(getString(R.string.text_cancel_tlo))
                            .setMessage(getString(R.string.text_cancel_tlo_desc))
                            .setPositiveButton(getString(R.string.text_yes_confirm), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    fragmentInteractionListener.onGcodeCommandReceived(GrblUtils.GCODE_CANCEL_TOOL_OFFSETS);
                                    fragmentInteractionListener.onGcodeCommandReceived(GrblUtils.GRBL_VIEW_GCODE_PARAMETERS_COMMAND);
                                }
                            })
                            .setNegativeButton(getString(R.string.text_no_confirm), null)
                            .show();
                }else{
                    EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_machine_not_idle)));
                }
            }
        });

        autoZeroAfterProbe = view.findViewById(R.id.auto_zero_after_probe);

        RelativeLayout probingHelp = view.findViewById(R.id.probing_help);
        probingHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showProbingHelp();
            }
        });

        return view;
    }

    private void doProbing(){
        if(machineStatus.getState().equals(Constants.MACHINE_STATUS_IDLE)){

            fragmentInteractionListener.onGcodeCommandReceived(GrblUtils.GRBL_VIEW_PARSER_STATE_COMMAND);

            String probeDistance = probingDistance.getText().toString();
            final String probeFeedRate = probingFeedRate.getText().toString();
            final Double distanceToProbe = machineStatus.getWorkPosition().getCordZ() - Double.parseDouble(probeDistance);
            probeStartPosition = machineStatus.getMachinePosition().getCordZ();

            // Wait for few milliseconds, just to make sure we got the parser state
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    String distanceMode = machineStatus.getParserState().distanceMode;
                    String unitSelection = machineStatus.getParserState().unitSelection;

                    fragmentInteractionListener.onGcodeCommandReceived("G38.3 Z" + distanceToProbe.toString() + " F" + probeFeedRate);
                    fragmentInteractionListener.onGcodeCommandReceived(distanceMode + unitSelection);
                }
            }, (Constants.GRBL_STATUS_UPDATE_INTERVAL + 100));


        }else{
            EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_machine_not_idle)));
        }
    }

    private void setProbingDistance(){
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final ViewGroup nullParent = null;
        View v = inflater.inflate(R.layout.dialog_input_decimal, nullParent, false);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setView(v);
        alertDialogBuilder.setTitle(getString(R.string.text_probing_distance));

        final EditText editText = v.findViewById(R.id.dialog_input_decimal);
        editText.setText(sharedPref.getString(getString(R.string.preference_probing_distance), "10.0"));
        editText.setSelection(editText.getText().length());

        alertDialogBuilder.setCancelable(true)
                .setPositiveButton(getString(R.string.text_yes_confirm), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String feedRate = editText.getText().toString();
                        sharedPref.edit().putString(getString(R.string.preference_probing_distance), feedRate).apply();
                        probingDistance.setText(feedRate);
                    }
                })
                .setNegativeButton(getString(R.string.text_cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        AlertDialog dialog = alertDialogBuilder.create();
        if(dialog.getWindow() != null){
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        dialog.show();
    }

    private void setProbingPlateThickness(){
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final ViewGroup nullParent = null;
        View v = inflater.inflate(R.layout.dialog_input_decimal, nullParent, false);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setView(v);
        alertDialogBuilder.setTitle(getString(R.string.text_touch_plate_thickness));

        final EditText editText = v.findViewById(R.id.dialog_input_decimal);
        editText.setText(sharedPref.getString(getString(R.string.preference_probing_plate_thickness), "10.0"));
        editText.setSelection(editText.getText().length());

        alertDialogBuilder.setCancelable(true)
                .setPositiveButton(getString(R.string.text_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String feedRate = editText.getText().toString();
                        sharedPref.edit().putString(getString(R.string.preference_probing_plate_thickness), feedRate).apply();
                        probingPlateThickness.setText(feedRate);
                    }
                })
                .setNegativeButton(getString(R.string.text_cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        AlertDialog dialog = alertDialogBuilder.create();
        if(dialog.getWindow() != null) dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();

    }

    private void setProbingFeedRate(){
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        final ViewGroup nullParent = null;
        View v = inflater.inflate(R.layout.dialog_input_decimal, nullParent, false);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setView(v);
        alertDialogBuilder.setTitle(getString(R.string.text_probing_feed_rate));

        final EditText editText = v.findViewById(R.id.dialog_input_decimal);
        editText.setText(sharedPref.getString(getString(R.string.preference_probing_feed_rate), "10.0"));
        editText.setSelection(editText.getText().length());

        alertDialogBuilder.setCancelable(true)
                .setPositiveButton(getString(R.string.text_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String feedRate = editText.getText().toString();
                        sharedPref.edit().putString(getString(R.string.preference_probing_feed_rate), feedRate).apply();
                        probingFeedRate.setText(feedRate);
                    }
                })
                .setNegativeButton(getString(R.string.text_cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        AlertDialog dialog = alertDialogBuilder.create();
        if(dialog.getWindow() != null) dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    private void showProbingHelp(){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity())
                .setTitle(getString(R.string.text_manual_tool_change))
                .setMessage(R.string.text_probing_help)
                .setPositiveButton(getString(R.string.text_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) { }
                })
                .setCancelable(false);

        alertDialogBuilder.show();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onGrblProbeEvent(GrblProbeEvent event){
        if(probeType == null || probeStartPosition == null) return;

        Answers.getInstance().logCustom(new CustomEvent("Probing")
                .putCustomAttribute("type", (probeType == 1) ? "G38.3" : "G43.1")
                .putCustomAttribute("status", event.getIsProbeSuccess() ? "success" : "failed"));

        if(!event.getIsProbeSuccess()){
            EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_probe_failed)));
            fragmentInteractionListener.onGcodeCommandReceived("G53G0Z" + probeStartPosition.toString());
            probeType = null;
            return;
        }

        if(probeType == Constants.PROBE_TYPE_NORMAL){
            if(autoZeroAfterProbe.isChecked()){
                Double probePlateThickness = Double.parseDouble(probingPlateThickness.getText().toString());
                fragmentInteractionListener.onGcodeCommandReceived("G53G0Z" + event.getProbeCordZ().toString());
                fragmentInteractionListener.onGcodeCommandReceived("G10L20P0Z" + probePlateThickness);
                autoZeroAfterProbe.setChecked(false);
                EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_probe_success_auto_zero)));
            }else{
                EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_probe_success)));
            }
        }

        if(probeType == Constants.PROBE_TYPE_TOOL_OFFSET){
            Double lastProbeCordZ = Math.abs(machineStatus.getLastProbePosition().getCordZ());
            Double currentProbeCordZ = Math.abs(event.getProbeCordZ());

            Double toolOffset =  lastProbeCordZ - currentProbeCordZ;
            fragmentInteractionListener.onGcodeCommandReceived("G43.1Z" + toolOffset.toString());
            fragmentInteractionListener.onGcodeCommandReceived(GrblUtils.GRBL_VIEW_GCODE_PARAMETERS_COMMAND);
            EventBus.getDefault().post(new UiToastEvent(getString(R.string.text_probe_success_with_tlo)));
        }

        fragmentInteractionListener.onGcodeCommandReceived("G53G0Z" + probeStartPosition.toString());
        probeStartPosition = null; probeType = null;
        machineStatus.setLastProbePosition(event.getProbePosition());
    }

}
