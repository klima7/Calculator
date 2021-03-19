package com.klima7.calculator;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CalculatorActivity extends AppCompatActivity {

    public static final String ADVANCED = "Advanced";
    public static final int DISPLAY_MAX_DIGITS = 20;
    public static final long CE_DELAY = 1000;

    private TextView display;

    private BigDecimal result = BigDecimal.valueOf(0);
    private BigDecimal memory = BigDecimal.valueOf(0);
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
            result = new BigDecimal(inState.getString("result"));
            memory = new BigDecimal(inState.getString("memory"));
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

        outState.putString("result", result.toPlainString());
        outState.putString("memory", memory.toPlainString());
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
            if (op.equalsIgnoreCase("sqrt")) result = BigDecimal.valueOf(Math.sqrt(result.doubleValue()));
            else if (op.equalsIgnoreCase("x^2")) result = result.multiply(result);
            else if (op.equalsIgnoreCase("sin")) result = BigDecimal.valueOf(Math.sin(result.doubleValue()));
            else if (op.equalsIgnoreCase("cos")) result = BigDecimal.valueOf(Math.cos(result.doubleValue()));
            else if (op.equalsIgnoreCase("tan")) result = BigDecimal.valueOf(Math.tan(result.doubleValue()));
            else if (op.equalsIgnoreCase("ln")) result = BigDecimal.valueOf(Math.log(result.doubleValue()));
            else if (op.equalsIgnoreCase("log")) result = BigDecimal.valueOf(Math.log10(result.doubleValue()));
            else if (op.equalsIgnoreCase("%")) result = result.divide(BigDecimal.valueOf(100));

            if(numberTooBig(result)) {
                String message = getResources().getString(R.string.result_too_big);
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                setNumber(0);
                result = BigDecimal.ZERO;
            }
            else {
                setNumber(result);
                pendingClean = true;
                currentOp = null;
            }

        } catch(IllegalArgumentException e) {
            String message = getResources().getString(R.string.invalid_operation);
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            setNumber(0);
            result = BigDecimal.ZERO;
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
            result = BigDecimal.ZERO;

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
        result = BigDecimal.ZERO;
        memory = BigDecimal.ZERO;
        setNumber(0);
    }

    public void plusMinusClicked(View view) {
        if(getNumber().equals(BigDecimal.ZERO))
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
        memory = BigDecimal.ZERO;
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
            BigDecimal operand = getNumber();
            BigDecimal opResult = BigDecimal.ZERO;
            if (pendingOp.equalsIgnoreCase("+"))
                opResult = result.add(operand);
            else if (pendingOp.equalsIgnoreCase("-"))
                opResult = result.subtract(operand);
            else if (pendingOp.equalsIgnoreCase("*"))
                opResult = result.multiply(operand);
            else if (pendingOp.equalsIgnoreCase("/")) {
                String ans = result.divide(operand, DISPLAY_MAX_DIGITS, RoundingMode.HALF_EVEN).toPlainString();
                ans = removeUnnecessaryZeros(ans);
                opResult = new BigDecimal(ans);
            }
            else if (pendingOp.equalsIgnoreCase("x^y"))
                opResult = result.pow(operand.intValue());
            pendingOp = null;

            if(numberTooBig(opResult)) {
                String message = getResources().getString(R.string.result_too_big);
                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                setNumber(0);
                result = BigDecimal.ZERO;
            }
            else {
                setNumber(opResult);
                result = opResult;
                pendingClean = true;
            }

        } catch(IllegalArgumentException | ArithmeticException e) {
            String message = getResources().getString(R.string.invalid_operation);
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
            setNumber(0);
            result = BigDecimal.ZERO;
            pendingOp = null;
            currentOp = null;
        }
    }

    boolean numberTooBig(BigDecimal decimal) {
        String text = decimal.toPlainString();
        int dotPos = text.contains(".") ? text.indexOf('.') : text.length();
        return dotPos > DISPLAY_MAX_DIGITS;
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

    private BigDecimal getNumber() {
        try {
            return new BigDecimal(getText());
        } catch(NumberFormatException e) {
            return BigDecimal.valueOf(0);
        }
    }

    private void setText(String text) {
        display.setText(text);
    }

    private void setNumber(double number) {
        setText(num2Str(number));
    }

    private void setNumber(BigDecimal number) {
        String text = number.toPlainString();
        if(text.length() > DISPLAY_MAX_DIGITS)
            text = text.substring(0, DISPLAY_MAX_DIGITS);
        setText(text);
    }

    private void cleanIfPending() {
        if(pendingClean) {
            pendingClean = false;
            setNumber(0);
        }
    }

    String removeUnnecessaryZeros(String number) {
        if(number.contains(".")) {
            while(number.charAt(number.length()-1) == '0') {
                number = number.substring(0, number.length()-1);
            }
        }
        return number;
    }

    private void debug() {
        Log.d("Hello", "pendingOp=" + pendingOp + " currentOp=" + currentOp + " result=" + result + " display=" + getText() + " clean=" + pendingClean);
    }
}
