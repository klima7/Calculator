package com.klima7.calculator;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CalculatorActivity extends AppCompatActivity {

    public static final String ADVANCED = "Advanced";
    public static final int DISPLAY_MAX_DIGITS = 20;
    public static final long CE_DELAY = 1000;

    private TextView display;

    private double result = 0;
    private double memory = 0;
    private String pendingOp = null;
    private String currentOp = null;
    private boolean pendingClean = false;
    private long lastClickTime = 0;

    @Override
    protected void onCreate(Bundle inState) {
        super.onCreate(inState);

        boolean advanced = getIntent().getBooleanExtra(CalculatorActivity.ADVANCED, true);
        setContentView(advanced ? R.layout.advanced_layout : R.layout.simple_layout);

        display = findViewById(R.id.calculator_display);
        setNumber(0);

        if(inState != null) {
            result = inState.getDouble("result");
            memory = inState.getDouble("memory");
            pendingOp = inState.getString("pendingOp");
            currentOp = inState.getString("currentOp");
            pendingClean = inState.getBoolean("pendingClean");
            lastClickTime = inState.getLong("lastClickTime");
            setText(inState.getString("display"));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putDouble("result", result);
        outState.putDouble("memory", memory);
        outState.putString("pendingOp", pendingOp);
        outState.putString("currentOp", currentOp);
        outState.putBoolean("pendingClean", pendingClean);
        outState.putLong("lastClickTime", lastClickTime);
        outState.putString("display", display.getText().toString());
    }

    public void numberClicked(View view) {
        cleanIfPending();

        if(getText().length() >= DISPLAY_MAX_DIGITS)
            return;

        changeCurrentOpToPending();

        Button button = (Button)view;
        int number = Integer.parseInt(button.getText().toString());

        if(getText().equals("0")) {
            if(number == 0) {
                return;
            }
            else {
                setText("");
            }
        }
        setText(getText() + number);
    }

    public void dotClicked(View view) {
        changeCurrentOpToPending();

        if(getText().contains("."))
            return;
        setText(getText() + ".");
    }

    public void twoArgumentOperationClicked(View view) {
        // Pending operation
        if(pendingOp != null) {
            performOperation();
        }

        // No pending operation
        else {
            result = getNumber();
            pendingClean = true;
        }

        Button button = (Button)view;
        currentOp = button.getText().toString();
    }

    public void singleArgumentOperationClicked(View view) {
        if(pendingOp != null) {
            performOperation();
        }

        Button button = (Button)view;
        String op = button.getText().toString();

        result = getNumber();

        try {
            if (op.equalsIgnoreCase("sqrt")) {
                if(result < 0) {
                    String message = getResources().getString(R.string.sqrt_error);
                    throw new IllegalArgumentException(message);
                }
                result = Math.sqrt(result);
            }
            else if (op.equalsIgnoreCase("x^2")) result = result*result;
            else if (op.equalsIgnoreCase("sin")) result = Math.sin(result);
            else if (op.equalsIgnoreCase("cos")) result = Math.cos(result);
            else if (op.equalsIgnoreCase("tan")) result = Math.tan(result);
            else if (op.equalsIgnoreCase("ln")) result = Math.log(result);
            else if (op.equalsIgnoreCase("log")) result = Math.log10(result);
            else if (op.equalsIgnoreCase("%")) result = result/100;

            setNumber(result);
            pendingClean = true;
            currentOp = null;

        } catch(IllegalArgumentException e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            setNumber(0);
            result = 0;
            pendingOp = null;
            currentOp = null;
            pendingClean = true;
        }
    }

    public void equalsClicked(View view) {
        if(pendingOp != null) {
            performOperation();
        }
    }

    public void bkspClicked(View view) {
        cleanIfPending();
        setText(getText().substring(0, getText().length()-1));
        if(getText().length() == 0 || getText().equals("-"))
            setNumber(0);
    }

    public void CClicked(View view) {

        long currentClickTime = System.currentTimeMillis();

        if(currentClickTime - lastClickTime < CE_DELAY) {
            setNumber(0);
            pendingOp = null;
            currentOp = null;
            pendingClean = false;
            result = 0;

            String message = getResources().getString(R.string.cclicked);
            Toast toast = Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT);
            toast.show();
        }
        else {
            setNumber(0);
        }

        lastClickTime = currentClickTime;
    }

    public void AcClicked(View view) {
        pendingOp = null;
        currentOp = null;
        pendingClean = false;
        result = 0;
        memory = 0;
        setNumber(0);
    }

    public void plusMinusClicked(View view) {
        double number = getNumber();
        if(number == 0)
            return;

        if(getText().startsWith("-"))
            setText(getText().substring(1));
        else
            setText("-" + getText());
    }

    public void memorySetClicked(View view) {
        memory = getNumber();
    }

    public void memoryClearClicked(View view) {
        memory = 0;
    }

    public void memoryRecallClicked(View view) {
        cleanIfPending();
        changeCurrentOpToPending();
        setNumber(memory);
    }

    private void changeCurrentOpToPending() {
        if(currentOp != null) {
            pendingOp = currentOp;
            currentOp = null;
        }
    }

    private void performOperation() {
        try {
            double operand = getNumber();
            double opResult = 0;
            if (pendingOp.equalsIgnoreCase("+"))
                opResult = result + operand;
            else if (pendingOp.equalsIgnoreCase("-"))
                opResult = result - operand;
            else if (pendingOp.equalsIgnoreCase("*"))
                opResult = result * operand;
            else if (pendingOp.equalsIgnoreCase("/")) {
                if(operand == 0) {
                    String message = getResources().getString(R.string.division_error);
                    throw new IllegalArgumentException(message);
                }
                opResult = result / operand;
            }
            else if (pendingOp.equalsIgnoreCase("x^y"))
                opResult = Math.pow(result, operand);
            pendingOp = null;

            setNumber(opResult);
            result = opResult;
            pendingClean = true;

        } catch(IllegalArgumentException e) {
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
            setNumber(0);
            result = 0;
            pendingOp = null;
            currentOp = null;
        }
    }

    private String num2Str(double number) {
        if(number % 1 == 0) {
            return "" + (long)number;
        }
        else {
            return "" + number;
        }
    }

    private String getText() {
        return display.getText().toString();
    }

    private double getNumber() {
        try {
            return Double.parseDouble(getText());
        } catch(NumberFormatException e) {
            return 0;
        }
    }

    private void setText(String text) {
        display.setText(text);
    }

    private void setNumber(double number) {
        setText(num2Str(number));
    }

    private void cleanIfPending() {
        if(pendingClean) {
            pendingClean = false;
            setNumber(0);
        }
    }

    private void debug() {
        Log.d("Hello", "pendingOp=" + pendingOp + " currentOp=" + currentOp + " result=" + result + " display=" + getText() + " clean=" + pendingClean);
    }
}
